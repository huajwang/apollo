    // Debounce function to limit the frequency of update requests
    function debounce(func, delay) {
      let debounceTimer;
      return function(...args) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => func.apply(this, args), delay);
      };
    }

    // Function to handle quantity changes
    const handleQuantityChange = debounce((itemId, quantity) => {
      fetch('/cart/update-quantity', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ itemId, quantity })
      })
      .then(response => {
        console.log('Raw response:', response);
        return response.text();
       })
      .then(data => {
          console.log('Quantity updated:', data);
          updateCartTotal();
          })
      .catch(error => console.error('Error updating quantity:', error));
    }, 500); // 500 ms debounce delay

    // Function to handle checkbox changes for adding/subtracting item amounts
    const handleCheckboxChange = (itemId, isChecked) => {
      fetch('/cart/update-total-on-checkbox', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ itemId, isChecked })
      })
      .then(response => {
        return response.json();
        })
      .then(data => {
                    console.log('Cart total updated:', data);
                    updateCartTotal();
                    })
      .catch(error => console.error('Error updating cart total:', error));
    };

    // Function to handle checkout
    function handleCheckout() {
      fetch('/cart/checkout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ action: 'checkout' })
      })
      .then(response => {
        if (!response.ok) {
            throw new Error('Checkout failed');
        }
        return response.json();
      })
      .then(data => {
        const orderId = data.orderId;  // Extract orderId from JSON response
        window.location.href = `/checkout?orderId=${orderId}`;
      })
      .catch(error => console.error('Error during checkout:', error));
    }

    function updateCartTotal() {
      fetch('/cart/total')
        .then(response => response.json())
        .then(data => {
            document.getElementById("cartTotal").textContent = `Total: $${data.toFixed(2)}`;
        })
        .catch(error => console.error('Error fetching cart total:', error));
    }


    function updateCheckoutButtonState() {
        const checkoutButton = document.querySelector(".checkout-btn");
         // Check if the "Checkout" button exists
         if (!checkoutButton) {
            return;
         }

        const selectedItems = document.querySelectorAll(".cart-item input[type='checkbox']:checked");

        // Disable the button if no items are selected
        checkoutButton.disabled = selectedItems.length === 0;

        // update the button's appearance (e.g., grayed-out style)
        if (checkoutButton.disabled) {
            checkoutButton.classList.add("disabled");
        } else {
            checkoutButton.classList.remove("disabled");
        }
    }

    // Ensure the checkout button state is updated on page load
    document.addEventListener("DOMContentLoaded", () => {
        updateCheckoutButtonState();

        // Add event listeners for checkboxes to dynamically update button state
        const checkboxes = document.querySelectorAll(".cart-item input[type='checkbox']");
        checkboxes.forEach(checkbox => {
            checkbox.addEventListener("change", () => {
                updateCheckoutButtonState();
            });
        });
    });
