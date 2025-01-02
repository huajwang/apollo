package com.goodfeel.nightgrass.web.admin

import com.goodfeel.nightgrass.dto.admin.AdminProductDto
import com.goodfeel.nightgrass.dto.admin.AdminProductRequest
import com.goodfeel.nightgrass.service.AliyunOssService
import com.goodfeel.nightgrass.service.admin.AdminProductService
import org.slf4j.LoggerFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/admin/products")
class ProductAdminController(
    private val adminProductService: AdminProductService,
    private val aliyunOssService: AliyunOssService
) {

    private val logger = LoggerFactory.getLogger(ProductAdminController::class.java)

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
        @RequestPart("mediaFiles", required = false) filePartFlux: Flux<FilePart>,
        @RequestPart("thumbnailFile", required = false) thumbnailFilePartMono: Mono<FilePart>
    ): Mono<String> {
        val thumbnailFileUrlMono = thumbnailFilePartMono.flatMap {
            aliyunOssService.uploadSingleFile(thumbnailFilePartMono)
        }.switchIfEmpty(Mono.just(""))

        return thumbnailFileUrlMono
        .flatMap { thumbnailFileUrl ->
            if (thumbnailFileUrl.isNotEmpty()) { // Do not override the existing imageUrl if no thumbnail image is provided
                adminProductRequest.imageUrl = thumbnailFileUrl
            }
            val filePartListMono = filePartFlux
                .filter { filePart -> filePart.filename().isNotBlank() } // Filter out empty file parts
                .collectList().flatMap { filePartList ->
                if (filePartList.isEmpty()) {
                    Mono.just(emptyList()) // Explicitly return an empty Mono when no files are uploaded
                } else {
                    aliyunOssService.uploadMultipleFiles(Flux.fromIterable(filePartList)).collectList()
                }
            }
            filePartListMono
                .flatMap { fileUrls ->
                    if (adminProductRequest.productId == null) {
                        adminProductService.createProduct(adminProductRequest, fileUrls)
                    } else {
                        adminProductService.updateProduct(
                            adminProductRequest.productId, adminProductRequest, fileUrls
                        )
                    }.thenReturn("redirect:/admin/products")
                }
        }.onErrorResume { ex ->
            logger.error("Error occurred: ${ex.message}")
            Mono.just("redirect:/admin-error-page")
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
            .flatMap { productDto ->
                model.addAttribute("product", productDto)
                adminProductService.getPhotoUrlsByProductId(productDto.productId!!)
                    .collectList()
                    .doOnNext {
                        model.addAttribute("photoUrls", it)
                    }
            }
            .thenReturn("admin/product-form")
    }

}
