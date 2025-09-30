package com.goodfeel.nightgrass.rest

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class FacebookDataController {

    @PostMapping("/facebook/delete-user-data")
    fun deleteUserData(@RequestBody payload: Map<String, String>): ResponseEntity<String> {
        val userId = payload["user_id"] ?: return ResponseEntity.badRequest().body("Invalid request")

        // Add logic to delete the user's data from your database
        // Example: userService.deleteUserDataByFacebookId(userId)

        return ResponseEntity.ok("User data deletion request received for user ID: $userId")
    }
}
