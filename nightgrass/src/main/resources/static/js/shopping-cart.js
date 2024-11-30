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
        const cartItems = document.querySelectorAll(".cart-item");

        // Disable the button if there are no items in the cart
        checkoutButton.disabled = cartItems.length === 0;
    }

    document.addEventListener("DOMContentLoaded", updateCheckoutButtonState);
