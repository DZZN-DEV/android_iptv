package com.enzoftware.androidiptv

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.enzoftware.androidiptv.adapter.ChannelListAdapter
import com.enzoftware.androidiptv.m3u.ChannelItem
import com.enzoftware.androidiptv.m3u.ChannelList
import com.enzoftware.androidiptv.m3u.Parser
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.FileNotFoundException
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var kryo: Kryo? = null
    private var cla: ChannelListAdapter? = null
    private lateinit var cl: ChannelList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/DZZN-DEV/android_iptv/master/sorted_channels.m3u8")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity, "Error fetching URL - " + e.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    cl = Parser.parse(response.body()!!.byteStream())
                    runOnUiThread {
                        cla = ChannelListAdapter(this@MainActivity, cl)
                        channel_list.adapter = cla
                        try {
                            val output = Output(openFileOutput("playlist.bin", MODE_PRIVATE))
                            kryo?.writeObject(output, cl)
                            output.close()
                        } catch (e: FileNotFoundException) {
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, "Error parsing M3U",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        })

        channel_list.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val ci = channel_list.adapter.getItem(position) as ChannelItem
                Log.d("CHANNEL_ITEM", ci.mediaUrl!!)
                val intent = Intent(this, FullScreenVideoPlayerActivity::class.java)
                val gson = Gson()
                val myJson = gson.toJson(cl.items)
                intent.putExtra("EXTRA_LIST", myJson)
                intent.putExtra("EXTRA_INDEX", position)
                Log.d("HOLA", myJson.toString())
                startActivity(intent)
            }

        search.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (cla != null && !cla!!.isEmpty)
                    cla!!.filter.filter(s)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        })

    }
}
