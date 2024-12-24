package com.goodfeel.nightgrass.web.admin

import com.goodfeel.nightgrass.dto.admin.AdminProductDto
import com.goodfeel.nightgrass.dto.admin.AdminProductRequest
import com.goodfeel.nightgrass.service.AliyunOssService
import com.goodfeel.nightgrass.service.admin.AdminProductService
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Paths

@Controller
@RequestMapping("/admin/products")
class ProductAdminController(
    private val adminProductService: AdminProductService,
    private val aliyunOssService: AliyunOssService
) {

    /**
     * Render the list of products for admin view.
     */
    @GetMapping
    fun showProductsPage(model: Model): Mono<String> {
        return adminProductService.getAllProducts()
            .collectList()
            .doOnNext { products ->
                model.addAttribute("products", products)
            }
            .thenReturn("admin/products")
    }

    /**
     * Render the product form for creating a new product.
     */
    @GetMapping("/new")
    fun showAddProductPage(model: Model): Mono<String> {
        return Mono.fromCallable {
            model.addAttribute("product", AdminProductDto())
            "admin/product-form"
        }
    }

    @PostMapping
    fun addNewProductOrUpdateProduct(
        @ModelAttribute adminProductRequest: AdminProductRequest,
        @RequestPart("mediaFiles") files: Flux<FilePart>
    ): Mono<String> {
        // Process the files
        val fileProcessing = files.flatMap { filePart ->
            val targetLocation = Paths.get("uploads", filePart.filename()) // Change "uploads" TODO
            filePart.transferTo(targetLocation).thenReturn(targetLocation.toString())
        }.collectList()

        return aliyunOssService.uploadMultipleFiles(files)
            .collectList()
            .flatMap { fileUrls ->
            if (adminProductRequest.productId == null) {
                adminProductService.createProduct(adminProductRequest, fileUrls)
            } else {
                adminProductService.updateProduct(
                    adminProductRequest.productId, adminProductRequest, fileUrls
                )
            }.thenReturn("redirect:/admin/products")
        }



    }

    /**
     * Render the product form for editing an existing product.
     */
    @GetMapping("/{id}/edit")
    fun showEditProductPage(@PathVariable id: Long, model: Model): Mono<String> {
        return adminProductService.getProductById(id)
            .map { adminProduct ->
                AdminProductDto(
                    productId = adminProduct.productId,
                    productName = adminProduct.productName,
                    description = adminProduct.description,
                    imageUrl = adminProduct.imageUrl,
                    price = adminProduct.price,
                    additionalInfoMap = adminProduct.getAdditionalInfoAsMap(),
                    category = adminProduct.category
                )
            }
            .doOnNext { productDto ->
                model.addAttribute("product", productDto)
            }
            .thenReturn("admin/product-form")
    }
}
