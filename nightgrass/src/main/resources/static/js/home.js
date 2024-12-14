  const wrapper = document.querySelector('.carousel-wrapper');
  const items = document.querySelectorAll('.carousel-item');
  const indicators = document.querySelectorAll('.carousel-indicators .indicator');
  let currentIndex = 0;

  function updateCarousel() {
    const offset = -(currentIndex * (items[0].clientWidth + 10)); // 10 is the gap
    wrapper.style.transform = `translateX(${offset}px)`;

    // Update indicators
    indicators.forEach((indicator, index) => {
      indicator.classList.toggle('active', index === currentIndex);
    });
  }

  // Next Slide
  function nextSlide() {
    currentIndex = (currentIndex + 1) % items.length;
    updateCarousel();
  }

  // Previous Slide
  function previousSlide() {
    currentIndex = (currentIndex - 1 + items.length) % items.length;
    updateCarousel();
  }

  // Handle Indicator Click
  indicators.forEach((indicator, index) => {
    indicator.addEventListener('click', () => {
      currentIndex = index;
      updateCarousel();
    });
  });

  // Swipe Navigation (Optional)
  let startX = 0;
  let isSwiping = false;

  wrapper.addEventListener('touchstart', (e) => {
    startX = e.touches[0].clientX;
    isSwiping = true;
  });

  wrapper.addEventListener('touchmove', (e) => {
    if (!isSwiping) return;
    const endX = e.touches[0].clientX;
    if (startX - endX > 50) {
      nextSlide();
      isSwiping = false;
    } else if (endX - startX > 50) {
      previousSlide();
      isSwiping = false;
    }
  });

  // Auto-slide every 5 seconds
  setInterval(nextSlide, 5000);
