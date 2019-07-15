package com.example.wifidirectdemo

import android.content.Context
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.getSystemService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mIntentFilter = IntentFilter()

    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel

    private var mWifiBR : WiFiDirectBroadcastReceiver? = null

    private var mConnected : Boolean = false
    private var mConnecting : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mManager = getSystemService( Context.WIFI_P2P_SERVICE ) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper, null)

        mWifiBR = WiFiDirectBroadcastReceiver( mManager, mChannel, this, this )

        // Indicates a change in the Wi-Fi P2P status.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        Toast.makeText( this, "WiFi P2P is enabled.", Toast.LENGTH_SHORT ).show()

        buttonWiFiOnOff.setOnClickListener( buttonWiFiOnOff_onClickListener )

        buttonScan.setOnClickListener( buttonScan_onClickListener )
        buttonScan.isEnabled = false

        buttonDisconnect.setOnClickListener( buttonDisconnect_onClickListener )
        buttonDisconnect.isEnabled = false

        listViewPeers.setOnItemClickListener( listViewPeers_onItemClickListener )

        updateButtonAndStatusWiFiOnOff( true )
    }

    override fun onResume()
    {
        super.onResume()

        mWifiBR?.also{ receiver -> registerReceiver( receiver, mIntentFilter ) }
    }

    override fun onPause()
    {
        super.onPause()

        mWifiBR?.also{ receiver -> unregisterReceiver( receiver ) }
    }

    private fun updateButtonAndStatusWiFiOnOff( chk : Boolean, enable : Boolean = false )
    {
        var en : Boolean = enable

        if( chk ) {
            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager

            en = wifi.isWifiEnabled
        }

        when ( en )
        {
            true -> {
                textViewStatus.text = "WiFi ON"
                buttonWiFiOnOff.text = "WiFi OFF"

                buttonScan.isEnabled = true
            }

            else -> {
                textViewStatus.text = "WiFi OFF"
                buttonWiFiOnOff.text = "WiFi ON"

                buttonScan.isEnabled = false
            }
        }
    }

    val buttonWiFiOnOff_onClickListener = object : View.OnClickListener
    {
        override fun onClick( v : View )
        {
            val wifi = getSystemService( Context.WIFI_SERVICE ) as WifiManager

            var en = wifi.isWifiEnabled

            en = !en

            wifi.isWifiEnabled = en

            updateButtonAndStatusWiFiOnOff( false, en )
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // スキャン 開始
    //

    val buttonScan_onClickListener = object : View.OnClickListener
    {
        override fun onClick( v: View )
        {
            mManager.discoverPeers( mChannel, object : WifiP2pManager.ActionListener
                {
                    override fun onSuccess()
                    {
                        textViewStatus.text = "discoverPeers succeeded"
                    }

                    override fun onFailure( rc : Int )
                    {
                        textViewStatus.text = "discoverPeers failed rc=" + rc
                    }
                }
            )
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // 切断
    //

    val buttonDisconnect_onClickListener = object : View.OnClickListener
    {
        override fun onClick( v: View )
        {
            mManager.removeGroup( mChannel, object : WifiP2pManager.ActionListener
                {
                    override fun onSuccess()
                    {
                        textViewStatus.text = "removePeers succeeded"

                        buttonDisconnect.isEnabled = false
                    }

                    override fun onFailure( rc : Int )
                    {
                        textViewStatus.text = "removePeers failed rc=" + rc
                    }
                }
            )
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // Peerデバイスの通知
    //

    var listOfWifiP2pDevice = arrayListOf<WifiP2pDevice>()

    val wifip2p_peerListListener = object : WifiP2pManager.PeerListListener
    {
        override fun onPeersAvailable( list: WifiP2pDeviceList? ) {

            if( list == null )
            {
                listOfWifiP2pDevice.clear()

                textViewStatus.text = "no list of peers."

                return
            }

            textViewStatus.text = "num of peerList = " + list.deviceList.size

            if( !list.equals(listOfWifiP2pDevice) ) {

                listOfWifiP2pDevice.clear()
                listOfWifiP2pDevice.addAll(list.deviceList)

                val listWifiP2pDeviceName = mutableListOf<String>()
                val listWifiP2pDevice = mutableListOf<WifiP2pDevice>()

                for (dev in list.deviceList) {
                    listWifiP2pDeviceName.add( dev.deviceName )
                    listWifiP2pDevice.add( dev )

                    Log.d( "WifiP2pDEMO", dev.deviceName )
                }

                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, listWifiP2pDeviceName )

                listViewPeers.adapter = adapter
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // Peerデバイスに接続 (listViewからの項目選択)
    //

    val listViewPeers_onItemClickListener = object : AdapterView.OnItemClickListener
    {
        override fun onItemClick( adptv : AdapterView<*>?, v : View?, pos: Int, id : Long)
        {
            if( mConnecting )
            {
                textViewStatus.text = "connecting"

                return
            }

            if( mConnected )
            {
                textViewStatus.text = "connected"

                return;
            }

            val dev : WifiP2pDevice = listOfWifiP2pDevice[pos]

            val cfg = WifiP2pConfig()

            cfg.deviceAddress = dev.deviceAddress

            mConnecting = true
            textViewStatus.text = "connecting to " + dev.deviceName

            mManager.connect( mChannel, cfg, wifip2p_connectListener )
        }
    }

    // mManager.connect(...)の結果通知
    val wifip2p_connectListener = object : WifiP2pManager.ActionListener
    {
        override fun onSuccess()
        {
            // mConnecting = false
            // mConnected  = true
        }

        override fun onFailure( reason : Int )
        {
            mConnecting = false
            mConnected  = false
            textViewStatus.text = "connection failured (" + reason +")"
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // Peerデバイスとの接続終了後のNetworkInfoの接続 (listViewからの項目選択)
    //
    /*
    val wifip2p_networkInfoListener = object : WifiP2pManager.NetworkInfoListener
    {
        override fun onNetworkInfoAvailable( info : NetworkInfo )
        {
        }
    }
    */

    fun wifip2p_disconnected()
    {
        mConnecting = false
        mConnected = false

        buttonDisconnect.isEnabled = false
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // Peerデバイスとの接続終了後のNetworkInfoの接続 (listViewからの項目選択)
    //
    val wifip2p_connectionInfoListener = object : WifiP2pManager.ConnectionInfoListener
    {
        override fun onConnectionInfoAvailable( info : WifiP2pInfo?)
        {
            info?.apply()
            {
                if( groupFormed )
                {
                    if( isGroupOwner )
                    {
                        // Host
                        textViewStatus.text = "This is Host."
                    }
                    else
                    {
                        // Client
                        textViewStatus.text = "This is Client. "
                    }

                    mConnecting = false
                    mConnected = true

                    buttonDisconnect.isEnabled = true
                }
            }
        }
    }
}
