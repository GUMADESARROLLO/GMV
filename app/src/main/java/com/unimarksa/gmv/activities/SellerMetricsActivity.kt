package com.unimarksa.gmv.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.unimarksa.gmv.R
import com.unimarksa.gmv.fragments.SellerMetricsFragment

class SellerMetricsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_metrics)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SellerMetricsFragment())
                .commit()
        }
    }
}
