package com.fphoenixcorneae.rulerview.demo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fphoenixcorneae.ext.logd
import com.fphoenixcorneae.rulerview.RulerView
import com.fphoenixcorneae.rulerview.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mViewBinding: ActivityMainBinding

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        mViewBinding.rvRuler.initRuler(
            RulerView.RulerType.HalfHour,
            60,
            1440,
            120
        ) {
            it.toString().logd()
        }
    }
}