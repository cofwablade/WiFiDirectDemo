package com.example.wifidirectdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity,
    private val context: Context
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val action: String? = intent.action

        when (action ) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activityWIFI_P2P_CONNECTION_CHANGED_ACTION

                val state = intent.getIntExtra( WifiP2pManager.EXTRA_WIFI_STATE, -1 )

                when( state )
                {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED ->
                    {
                        Toast.makeText( context, "WiFi P2P is enabled.", Toast.LENGTH_SHORT ).show()
                    }

                    WifiP2pManager.WIFI_P2P_STATE_DISABLED ->
                    {
                        Toast.makeText( context, "WiFi P2P is disabled.", Toast.LENGTH_SHORT ).show()
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers

                manager.requestPeers( channel, activity.wifip2p_peerListListener )

                Toast.makeText( context, "WIFI_P2P_PEERS_CHANGED_ACTION", Toast.LENGTH_SHORT ).show()
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections

                // manager.requestNetworkInfo( channel, activity.wifip2p_networkInfoListener )
                val networkInfo  = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo

                if( networkInfo.isConnected )
                {
                    manager.requestConnectionInfo( channel, activity.wifip2p_connectionInfoListener )
                }
                else
                {
                    activity.wifip2p_disconnected()
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
            }
        }
    }
}