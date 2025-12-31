// Navbar scroll effect
window.addEventListener("scroll", () => {
    const navbar = document.getElementById("mainNav")
    if (!navbar) return
    if (window.scrollY > 50) {
        navbar.classList.add("scrolled")
    } else {
        navbar.classList.remove("scrolled")
    }
})

// Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
    anchor.addEventListener("click", function (e) {
        const href = this.getAttribute("href")
        
        // Bỏ qua nếu href chỉ là "#" (dropdown, modal, etc.)
        if (!href || href === "#") {
            return
        }
        
        e.preventDefault()
        const target = document.querySelector(href)
        if (target) {
            target.scrollIntoView({
                behavior: "smooth",
                block: "start",
            })

            // Close mobile menu if open
            const navbarCollapse = document.querySelector(".navbar-collapse")
            if (navbarCollapse && navbarCollapse.classList.contains("show")) {
                navbarCollapse.classList.remove("show")
            }
        }
    })
})

// Intersection Observer for fade-in animations
const observerOptions = {
    threshold: 0.1,
    rootMargin: "0px 0px -50px 0px",
}

const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = "1"
            entry.target.style.transform = "translateY(0)"
        }
    })
}, observerOptions)

// Observe all fade-in elements
document.addEventListener("DOMContentLoaded", () => {
    const fadeElements = document.querySelectorAll(
        ".fade-in, .fade-in-delay, .fade-in-delay-1, .fade-in-delay-2, .fade-in-delay-3",
    )
    fadeElements.forEach((el) => observer.observe(el))
})

// Add active state to navbar links based on scroll position
window.addEventListener("scroll", () => {
    const sections = document.querySelectorAll("section[id]")
    const navLinks = document.querySelectorAll(".navbar-nav .nav-link")

    let current = ""
    sections.forEach((section) => {
        const sectionTop = section.offsetTop
        const sectionHeight = section.clientHeight
        if (window.scrollY >= sectionTop - 200) {
            current = section.getAttribute("id")
        }
    })

    navLinks.forEach((link) => {
        link.classList.remove("active")
        if (link.getAttribute("href") === "#" + current) {
            link.classList.add("active")
        }
    })
})

// Set active navbar link based on current page (works for separate HTML files)
document.addEventListener('DOMContentLoaded', () => {
    try {
        const navLinks = document.querySelectorAll('.navbar-nav .nav-link')
        const path = window.location.pathname || ''
        // get filename (e.g., about.html). If root, default to index.html
        let page = path.substring(path.lastIndexOf('/') + 1)
        if (!page) page = 'index.html'

        navLinks.forEach((link) => {
            const href = link.getAttribute('href') || ''
            // normalize href by removing any query/hash and any path
            const hrefNoHash = href.split('#')[0].split('?')[0]
            const hrefFile = hrefNoHash.substring(hrefNoHash.lastIndexOf('/') + 1)
            if (hrefFile === page) {
                link.classList.add('active')
            }
            // special case: link to index without filename (e.g., './' or '/')
            if ((href === '' || href === './' || href === '/' ) && page === 'index.html') {
                link.classList.add('active')
            }
        })
    } catch (e) {
        // fail silently
        console.warn('Nav active detection error', e)
    }
})

// Quiz option selection
document.querySelectorAll(".quiz-option").forEach((option) => {
    option.addEventListener("click", function () {
        const radio = this.querySelector('input[type="radio"]')
        if (radio) {
            radio.checked = true
        }
    })
})

// Pricing card hover effect enhancement
document.querySelectorAll(".pricing-card").forEach((card) => {
    card.addEventListener("mouseenter", function () {
        this.style.borderColor = "var(--color-primary)"
    })

    card.addEventListener("mouseleave", function () {
        if (!this.classList.contains("pricing-card-popular")) {
            this.style.borderColor = "var(--color-gray-light)"
        }
    })
})

// Add loading animation for images
document.addEventListener("DOMContentLoaded", () => {
    const images = document.querySelectorAll("img")
    images.forEach((img) => {
        img.addEventListener("load", function () {
            this.style.opacity = "1"
        })
    })
})

// Simple auth-aware navbar on index.html
document.addEventListener("DOMContentLoaded", async () => {
    try {
        const accessToken = localStorage.getItem("accessToken")
        const refreshToken = localStorage.getItem("refreshToken")

        const guestItems = document.querySelectorAll(".guest-only")
        const authItems = document.querySelectorAll(".auth-only")
        const avatarImg = document.getElementById("navUserAvatar")
        const logoutBtn = document.getElementById("navLogoutBtn")

        const showGuest = () => {
            guestItems.forEach(el => el.classList.remove("d-none"))
            authItems.forEach(el => el.classList.add("d-none"))
        }
        const showAuth = (avatarUrl) => {
            guestItems.forEach(el => el.classList.add("d-none"))
            authItems.forEach(el => el.classList.remove("d-none"))
            if (avatarImg && avatarUrl) {
                avatarImg.src = avatarUrl
            }
        }

        // Nếu không có token thì để chế độ guest
        if (!accessToken || !refreshToken) {
            showGuest()
            return
        }

        // Thử gọi /api/auth/me để lấy thông tin user (dùng apiClient để auto-refresh)
        const data = await window.apiClient.get("/api/auth/me").catch(() => null)
        
        if (!data) {
            // Token không hợp lệ / hết hạn
            showGuest()
            return
        }
        const avatarUrl = data && data.avatarUrl ? data.avatarUrl : 
                         `https://ui-avatars.com/api/?name=${encodeURIComponent(data?.fullName || 'User')}&size=80&background=1a4b84&color=fff`

        showAuth(avatarUrl)

        // Xử lý logout: gọi API logout + xoá token + reload
        if (logoutBtn) {
            logoutBtn.addEventListener("click", async (e) => {
                e.preventDefault()
                try {
                    const token = localStorage.getItem("refreshToken")
                    if (token) {
                        await fetch("/api/auth/logout", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({ refreshToken: token })
                        })
                    }
                } catch (err) {
                    console.warn("Logout error", err)
                } finally {
                    localStorage.removeItem("accessToken")
                    localStorage.removeItem("refreshToken")
                    window.location.href = "/index.html"
                }
            })
        }
    } catch (err) {
        console.warn("Navbar auth init error", err)
    }
})

// Console log for debugging
console.log("[v0] AI Luật website loaded successfully")
