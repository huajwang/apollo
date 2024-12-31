document.addEventListener("DOMContentLoaded", function () {
    const { orderTotalFinal, orderId } = CheckoutConfig;
    const submitButton = document.getElementById("submit-order-button");
    const customerName = document.getElementById("customerName");
    const phone = document.getElementById("phone");
    const address = document.getElementById("address");

    // Disable the button initially
    submitButton.disabled = true;

    // Function to display error messages
    function showError(inputElement, message) {
        let errorElement = inputElement.nextElementSibling;
        if (!errorElement || !errorElement.classList.contains("error-message")) {
            // Create an error message element if it doesn't exist
            errorElement = document.createElement("div");
            errorElement.classList.add("error-message");
            inputElement.parentElement.appendChild(errorElement);
        }
        errorElement.textContent = message;
        inputElement.classList.add("invalid");
    }

    // Function to clear error messages
    function clearError(inputElement) {
        let errorElement = inputElement.nextElementSibling;
        if (errorElement && errorElement.classList.contains("error-message")) {
            errorElement.textContent = "";
        }
        inputElement.classList.remove("invalid");
    }

    // Validation function
    function validateForm() {
        const isNameValid = customerName.value.trim() !== "";
        const isPhoneValid = /^[0-9\-\(\)\s]+$/.test(phone.value.trim()); // Simple phone validation
        const isAddressValid = address.value.trim() !== "";

        // Handle name validation
        if (!isNameValid) {
            showError(customerName, "Name is required.");
        } else {
            clearError(customerName);
        }

        // Handle phone validation
        if (!isPhoneValid) {
            showError(phone, "Invalid phone number.");
        } else {
            clearError(phone);
        }

        // Handle address validation
        if (!isAddressValid) {
            showError(address, "Address is required.");
        } else {
            clearError(address);
        }

        // Enable the button only if all fields are valid
        submitButton.disabled = !(isNameValid && isPhoneValid && isAddressValid);
    }

    // Attach event listeners to input fields
    customerName.addEventListener("input", validateForm);
    phone.addEventListener("input", validateForm);
    address.addEventListener("input", validateForm);

    // Perform a final validation on form submit
    submitButton.addEventListener("click", function () {
        if (submitButton.disabled) {
            alert("Please fill in all required fields correctly.");
            validateForm(); // Ensure error messages are displayed
            return;
        }

        fetch("/pay/create-checkout-session", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({
                amount: orderTotalFinal,
                orderId: orderId,
                contactName: customerName.value.trim(),
                contactPhone: phone.value.trim(),
                deliveryAddress: address.value.trim(),
            }),
        })
        .then((response) => {
            if (!response.ok) {
                return response.text().then((errorText) => {
                    throw new Error("Network response was not ok: " + errorText);
                });
            }
            return response.text();
        })
        .then((sessionUrl) => {
            window.location = sessionUrl;
        })
        .catch((error) => console.error("Error: ", error));
    });
});
