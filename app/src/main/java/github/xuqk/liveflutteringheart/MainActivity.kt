package github.xuqk.liveflutteringheart

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created By：XuQK
 * Created Date：2019-12-17 12:57
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fluttering_heart.init(24f, 4000f, 1.5f, R.drawable.heart, R.drawable.heart1, R.drawable.heart2, R.drawable.heart3, R.drawable.heart4, R.drawable.heart5)
        btn.setOnClickListener {
            fluttering_heart.shoot()
        }
    }
}
