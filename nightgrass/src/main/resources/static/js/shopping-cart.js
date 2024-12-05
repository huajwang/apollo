    // Debounce function to limit the frequency of update requests
    function debounce(func, delay) {
      let debounceTimer;
      return function(...args) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => func.apply(this, args), delay);
      };
    }

    // Cart object to manage cart state
    const cart = {
      itemCount: 0,
      items: [],
      totalPrice: 0,

      updateItemCount() {
          this.itemCount = this.items.reduce((total, item) => {
              return item.selected ? total + item.quantity : total;
          }, 0);
          this.updateItemCountUI();
      },

      // Method to update the item count in the UI
      updateItemCountUI() {
        const itemCountElement = document.getElementById("cartItemCount");
        if (itemCountElement) {
          itemCountElement.textContent = this.itemCount;
        }
      },

      // Method to increment item count
      incrementItemCount() {
        this.itemCount += 1;
        this.updateItemCountUI();
      },

      // Method to decrement item count
      decrementItemCount() {
        if (this.itemCount > 0) {
          this.itemCount -= 1;
          this.updateItemCountUI();
        }
      },

      // Method to update total price dynamically
      updateTotal() {
        console.log("updateTotal() data items: ", this.items);
        this.totalPrice = this.items.reduce((total, item) => {
          if (item.selected) {
            return total + item.quantity * item.price;
          }
          return total;
        }, 0);
        this.updateTotalUI();
      },

      // Method to update the total price in the UI
      updateTotalUI() {
        const totalElement = document.getElementById("cartTotal");
        if (totalElement) {
          totalElement.textContent = `Total: $${this.totalPrice.toFixed(2)}`;
        }
      },

      // Method to update a single item's quantity
      updateItemQuantity(itemId, quantity) {
        const item = this.items.find(item => item.id === itemId);
        if (item) {
          item.quantity = quantity;
          this.updateTotal();
        }
      },

      // Method to toggle an item's checked status
      toggleItemChecked(itemId, isChecked) {
        const item = this.items.find(item => item.id === itemId);
        if (item) {
          item.isChecked = isChecked;
          this.updateTotal();
        }
      }
    };

    const handleQuantityChange = debounce((itemId, quantity) => {
      // Optimistically update locally
      const previousQuantity = cart.items.find(item => item.id === itemId)?.quantity || 0;
      cart.updateItemQuantity(itemId, quantity);

      fetch('/cart/update-quantity', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ itemId, quantity })
      })
        .then(response => {
          if (!response.ok) {
            throw new Error('Failed to update quantity');
          }
          return response.json();
        })
        .then(data => {
          if (data.error) {
            console.error('Error updating quantity:', data.message);
            alert('Error updating quantity. Please try again.');
            // Revert to the previous quantity on error
            cart.updateItemQuantity(itemId, previousQuantity);
          } else {
            cart.items = data.items;
            cart.updateTotal();
          }
        })
        .catch(error => {
          console.error('Error updating quantity:', error);
          alert('An unexpected error occurred. Please try again.');
          // Revert to the previous quantity on error
          cart.updateItemQuantity(itemId, previousQuantity);
        });
    }, 500);


    const handleCheckboxChange = (itemId, isChecked) => {
      // Update the local cart state optimistically
      cart.toggleItemChecked(itemId, isChecked);
      cart.updateTotal();

      fetch('/cart/update-total-on-checkbox', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ itemId, isChecked })
      })
        .then(response => response.json())
        .then(data => {
          if (data.error) {
            console.error('Error updating cart total on checkbox change:', data.message);
            // Revert the local changes if the backend operation fails
            cart.toggleItemChecked(itemId, !isChecked);
            cart.updateTotal();
            alert('An error occurred while updating the cart. Please try again.');
          } else {
            // sync the cart state with the backend
            console.log("checkbox Receive data items: ", data.items)
            cart.items = data.items;
            cart.updateItemCount();
            cart.updateTotal();
          }
        })
        .catch(error => {
          console.error('Error updating cart total on checkbox change:', error);
          // Revert the local changes in case of an unexpected error
          cart.toggleItemChecked(itemId, !isChecked);
          cart.updateTotal();
          alert('An unexpected error occurred. Please try again.');
        });
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

    function handleRemoveItem(itemId) {
        fetch('/cart/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ itemId })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to remove the item from the cart.');
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                console.error(data.message);
                alert('Error removing item: ' + data.message);
            } else {
                // Update the UI to reflect the removal
                document.querySelector(`#selected_${itemId}`).closest('.cart-item').remove();
                console.log("cart items: ", data.items);
                cart.items = data.items;
                cart.updateItemCount();
                cart.updateTotal();
            }
        })
        .catch(error => {
            console.error('Unexpected error:', error);
            alert('An unexpected error occurred. Please try again.');
        });
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

// Initialize cart items on page load
document.addEventListener("DOMContentLoaded", () => {
  fetch('/cart/items')
    .then(response => response.json())
    .then(data => {
      cart.items = data.items;
      cart.updateItemCount();
      cart.updateTotal();
    })
    .catch(error => console.error('Error fetching cart items:', error));
});