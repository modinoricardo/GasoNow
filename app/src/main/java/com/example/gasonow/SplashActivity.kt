package com.example.gasonow

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.gasonow.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        runSplashAnimation()
    }

    private fun runSplashAnimation() {
        val iconScaleX = ObjectAnimator.ofFloat(binding.ivSplashIcon, "scaleX", 0f, 1f).apply {
            duration = 520
            interpolator = OvershootInterpolator(1.6f)
        }
        val iconScaleY = ObjectAnimator.ofFloat(binding.ivSplashIcon, "scaleY", 0f, 1f).apply {
            duration = 520
            interpolator = OvershootInterpolator(1.6f)
        }
        val iconAlpha = ObjectAnimator.ofFloat(binding.ivSplashIcon, "alpha", 0f, 1f).apply {
            duration = 300
        }
        val textSlide = ObjectAnimator.ofFloat(binding.tvSplashName, "translationY", 60f, 0f).apply {
            duration = 450
            startDelay = 380
            interpolator = DecelerateInterpolator()
        }
        val textAlpha = ObjectAnimator.ofFloat(binding.tvSplashName, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 380
        }
        val subtitleAlpha = ObjectAnimator.ofFloat(binding.tvSplashSubtitle, "alpha", 0f, 1f).apply {
            duration = 350
            startDelay = 620
        }

        AnimatorSet().apply {
            playTogether(iconScaleX, iconScaleY, iconAlpha, textSlide, textAlpha, subtitleAlpha)
            start()
        }

        binding.root.postDelayed({
            val options = ActivityOptions.makeCustomAnimation(
                this, android.R.anim.fade_in, android.R.anim.fade_out
            )
            startActivity(Intent(this, MainActivity::class.java), options.toBundle())
        }, 1750)
    }
}
