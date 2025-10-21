// 4D Water Wallpaper - Self-contained version
(function() {
    'use strict';
    
    // Configuration
    const CONFIG = {
        fishCount: 15,
        particleCount: 300,
        rippleDuration: 2000,
        maxRipples: 10
    };
    
    // Global state
    let canvas, ctx;
    let width, height;
    let fishes = [];
    let particles = [];
    let ripples = [];
    let animationFrame;
    let time = 0;
    
    // Fish class
    class Fish {
        constructor() {
            this.reset();
            this.hue = Math.random() * 360;
            this.size = Math.random() * 15 + 20;
            this.speed = Math.random() * 0.5 + 0.5;
            this.phase = Math.random() * Math.PI * 2;
        }
        
        reset() {
            this.x = Math.random() * width;
            this.y = height * 0.4 + Math.random() * height * 0.5;
            this.vx = (Math.random() - 0.5) * 2;
            this.vy = (Math.random() - 0.5) * 0.5;
            this.angle = Math.atan2(this.vy, this.vx);
        }
        
        update(dt) {
            // Update position
            this.x += this.vx * this.speed;
            this.y += this.vy * this.speed;
            
            // Boundary checking with wraparound
            if (this.x < -50) this.x = width + 50;
            if (this.x > width + 50) this.x = -50;
            if (this.y < height * 0.3) this.y = height * 0.3;
            if (this.y > height * 0.9) this.y = height * 0.9;
            
            // Random direction changes
            if (Math.random() < 0.01) {
                this.vx += (Math.random() - 0.5) * 0.5;
                this.vy += (Math.random() - 0.5) * 0.2;
                
                // Normalize velocity
                const mag = Math.sqrt(this.vx * this.vx + this.vy * this.vy);
                this.vx = (this.vx / mag) * 2;
                this.vy = (this.vy / mag) * 2;
            }
            
            // Update angle
            this.angle = Math.atan2(this.vy, this.vx);
        }
        
        draw(ctx, time) {
            ctx.save();
            ctx.translate(this.x, this.y);
            ctx.rotate(this.angle);
            
            // Add depth effect (parallax)
            const depth = (this.y - height * 0.3) / (height * 0.6);
            const scale = 0.5 + depth * 0.5;
            ctx.scale(scale, scale);
            
            // Tail wagging
            const tailWag = Math.sin(time * 0.005 * this.speed + this.phase) * 0.3;
            
            // Fish body
            ctx.fillStyle = `hsl(${this.hue}, 80%, 60%)`;
            ctx.beginPath();
            ctx.ellipse(0, 0, this.size, this.size * 0.6, 0, 0, Math.PI * 2);
            ctx.fill();
            
            // Tail
            ctx.save();
            ctx.rotate(tailWag);
            ctx.beginPath();
            ctx.moveTo(-this.size * 0.8, 0);
            ctx.lineTo(-this.size * 1.3, -this.size * 0.4);
            ctx.lineTo(-this.size * 1.3, this.size * 0.4);
            ctx.closePath();
            ctx.fillStyle = `hsl(${this.hue}, 70%, 50%)`;
            ctx.fill();
            ctx.restore();
            
            // Eye
            ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
            ctx.beginPath();
            ctx.arc(this.size * 0.4, -this.size * 0.2, this.size * 0.15, 0, Math.PI * 2);
            ctx.fill();
            
            ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
            ctx.beginPath();
            ctx.arc(this.size * 0.4, -this.size * 0.2, this.size * 0.08, 0, Math.PI * 2);
            ctx.fill();
            
            // Top fin
            ctx.fillStyle = `hsl(${this.hue}, 70%, 50%)`;
            ctx.beginPath();
            ctx.moveTo(this.size * 0.2, -this.size * 0.6);
            ctx.lineTo(0, -this.size * 0.9);
            ctx.lineTo(-this.size * 0.2, -this.size * 0.6);
            ctx.closePath();
            ctx.fill();
            
            // Side fins
            ctx.beginPath();
            ctx.ellipse(-this.size * 0.1, this.size * 0.5, this.size * 0.3, this.size * 0.2, -0.5, 0, Math.PI * 2);
            ctx.fill();
            
            ctx.restore();
        }
    }
    
    // Particle class for underwater atmosphere
    class Particle {
        constructor() {
            this.reset();
        }
        
        reset() {
            this.x = Math.random() * width;
            this.y = Math.random() * height;
            this.size = Math.random() * 2 + 1;
            this.speed = Math.random() * 0.2 + 0.1;
            this.opacity = Math.random() * 0.3 + 0.2;
        }
        
        update() {
            this.y -= this.speed;
            this.x += Math.sin(time * 0.001 + this.y * 0.01) * 0.5;
            
            if (this.y < 0) {
                this.y = height;
                this.x = Math.random() * width;
            }
        }
        
        draw(ctx) {
            ctx.fillStyle = `rgba(100, 150, 255, ${this.opacity})`;
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
            ctx.fill();
        }
    }
    
    // Ripple class for water surface interaction
    class Ripple {
        constructor(x, y) {
            this.x = x;
            this.y = y;
            this.startTime = Date.now();
            this.maxRadius = 150;
        }
        
        update() {
            const elapsed = Date.now() - this.startTime;
            return elapsed < CONFIG.rippleDuration;
        }
        
        draw(ctx) {
            const elapsed = Date.now() - this.startTime;
            const progress = elapsed / CONFIG.rippleDuration;
            const radius = this.maxRadius * progress;
            const opacity = 1 - progress;
            
            // Draw multiple rings for better effect
            for (let i = 0; i < 3; i++) {
                const ringRadius = radius - i * 20;
                if (ringRadius > 0) {
                    ctx.strokeStyle = `rgba(150, 200, 255, ${opacity * 0.5})`;
                    ctx.lineWidth = 3;
                    ctx.beginPath();
                    ctx.arc(this.x, this.y, ringRadius, 0, Math.PI * 2);
                    ctx.stroke();
                }
            }
        }
    }
    
    // Splash particle
    class SplashParticle {
        constructor(x, y) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 8;
            this.vy = -Math.random() * 5 - 3;
            this.life = 1.0;
            this.decay = 0.02;
            this.size = Math.random() * 3 + 2;
        }
        
        update() {
            this.x += this.vx;
            this.y += this.vy;
            this.vy += 0.3; // Gravity
            this.life -= this.decay;
            return this.life > 0;
        }
        
        draw(ctx) {
            ctx.fillStyle = `rgba(150, 220, 255, ${this.life * 0.8})`;
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
            ctx.fill();
        }
    }
    
    let splashParticles = [];
    
    // Initialize the application
    function init() {
        canvas = document.createElement('canvas');
        canvas.style.position = 'absolute';
        canvas.style.top = '0';
        canvas.style.left = '0';
        canvas.style.width = '100%';
        canvas.style.height = '100%';
        
        const container = document.getElementById('container');
        container.appendChild(canvas);
        
        ctx = canvas.getContext('2d');
        
        // Set canvas size
        resize();
        
        // Create fishes
        for (let i = 0; i < CONFIG.fishCount; i++) {
            fishes.push(new Fish());
        }
        
        // Create particles
        for (let i = 0; i < CONFIG.particleCount; i++) {
            particles.push(new Particle());
        }
        
        // Event listeners
        window.addEventListener('resize', resize);
        canvas.addEventListener('click', handleInteraction);
        canvas.addEventListener('touchstart', handleInteraction);
        
        // Hide loading screen
        document.getElementById('loading').style.display = 'none';
        
        // Start animation
        animate();
    }
    
    // Handle window resize
    function resize() {
        width = window.innerWidth;
        height = window.innerHeight;
        canvas.width = width;
        canvas.height = height;
        
        // Reset fish positions if needed
        fishes.forEach(fish => {
            if (fish.x > width) fish.x = width;
            if (fish.y > height) fish.y = height * 0.7;
        });
    }
    
    // Handle interaction (touch/click)
    function handleInteraction(e) {
        e.preventDefault();
        
        let x, y;
        if (e.type === 'touchstart') {
            x = e.touches[0].clientX;
            y = e.touches[0].clientY;
        } else {
            x = e.clientX;
            y = e.clientY;
        }
        
        // Create ripple
        if (ripples.length >= CONFIG.maxRipples) {
            ripples.shift();
        }
        ripples.push(new Ripple(x, y));
        
        // Create splash particles
        for (let i = 0; i < 15; i++) {
            splashParticles.push(new SplashParticle(x, y));
        }
    }
    
    // Draw water surface with waves
    function drawWaterSurface() {
        const waveHeight = 30;
        const waveFrequency = 0.02;
        
        // Draw water gradient
        const gradient = ctx.createLinearGradient(0, 0, 0, height);
        gradient.addColorStop(0, 'rgba(0, 100, 180, 0.3)');
        gradient.addColorStop(0.3, 'rgba(0, 80, 150, 0.5)');
        gradient.addColorStop(1, 'rgba(0, 40, 80, 0.8)');
        
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);
        
        // Draw animated waves on surface
        ctx.save();
        ctx.globalCompositeOperation = 'lighter';
        
        for (let layer = 0; layer < 3; layer++) {
            ctx.beginPath();
            ctx.moveTo(0, height * 0.25);
            
            for (let x = 0; x <= width; x += 5) {
                const y = height * 0.25 + 
                    Math.sin(x * waveFrequency + time * 0.001 + layer * 0.5) * waveHeight +
                    Math.sin(x * waveFrequency * 0.5 + time * 0.002 + layer) * waveHeight * 0.5;
                ctx.lineTo(x, y);
            }
            
            ctx.lineTo(width, 0);
            ctx.lineTo(0, 0);
            ctx.closePath();
            
            const alpha = 0.1 - layer * 0.02;
            ctx.fillStyle = `rgba(100, 180, 255, ${alpha})`;
            ctx.fill();
        }
        
        ctx.restore();
    }
    
    // Draw light rays
    function drawLightRays() {
        ctx.save();
        ctx.globalCompositeOperation = 'lighter';
        
        for (let i = 0; i < 5; i++) {
            const x = (width / 6) * (i + 1) + Math.sin(time * 0.0005 + i) * 100;
            
            const gradient = ctx.createLinearGradient(x, 0, x, height);
            gradient.addColorStop(0, 'rgba(150, 200, 255, 0.1)');
            gradient.addColorStop(0.5, 'rgba(100, 150, 200, 0.05)');
            gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
            
            ctx.fillStyle = gradient;
            ctx.fillRect(x - 30, 0, 60, height);
        }
        
        ctx.restore();
    }
    
    // Main animation loop
    function animate() {
        time = Date.now();
        
        // Clear canvas
        ctx.fillStyle = '#001a33';
        ctx.fillRect(0, 0, width, height);
        
        // Draw underwater environment
        drawWaterSurface();
        drawLightRays();
        
        // Update and draw particles
        particles.forEach(particle => {
            particle.update();
            particle.draw(ctx);
        });
        
        // Sort fishes by Y position for proper depth
        fishes.sort((a, b) => a.y - b.y);
        
        // Update and draw fishes
        fishes.forEach(fish => {
            fish.update(time);
            fish.draw(ctx, time);
        });
        
        // Update and draw ripples
        ripples = ripples.filter(ripple => {
            const alive = ripple.update();
            if (alive) ripple.draw(ctx);
            return alive;
        });
        
        // Update and draw splash particles
        splashParticles = splashParticles.filter(particle => {
            const alive = particle.update();
            if (alive) particle.draw(ctx);
            return alive;
        });
        
        // Draw water surface shimmer
        ctx.save();
        ctx.globalCompositeOperation = 'lighter';
        const shimmer = Math.sin(time * 0.003) * 0.05 + 0.05;
        ctx.fillStyle = `rgba(150, 200, 255, ${shimmer})`;
        
        ctx.beginPath();
        ctx.moveTo(0, height * 0.25);
        for (let x = 0; x <= width; x += 10) {
            const y = height * 0.25 + Math.sin(x * 0.02 + time * 0.002) * 20;
            ctx.lineTo(x, y);
        }
        ctx.lineTo(width, 0);
        ctx.lineTo(0, 0);
        ctx.closePath();
        ctx.fill();
        ctx.restore();
        
        animationFrame = requestAnimationFrame(animate);
    }
    
    // Start when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
