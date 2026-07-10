package com.example.ufitoolsremote.easytier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasyTierRuntimeTest {
    @Test
    fun parseStatus_usesRunningInstancesFallback_whenDetailedStatusIsBlank() {
        val status = EasyTierRuntime.parseStatus(
            raw = "",
            runningInstancesRaw = """{"home":"instance-1","office":"instance-2"}"""
        )

        assertEquals(2, status.instanceCount)
        assertEquals(2, status.runningCount)
        assertTrue(status.instances.all { it.running })
    }

    @Test
    fun parseStatus_keepsDetailedStatus_whenPresent() {
        val status = EasyTierRuntime.parseStatus(
            raw = """{"map":{"instance-1":{"running":true,"peers":[],"routes":[],"my_node_info":{"hostname":"home"}}}}""",
            runningInstancesRaw = """{"home":"instance-1"}"""
        )

        assertEquals(1, status.instanceCount)
        assertEquals(1, status.runningCount)
        assertEquals("home", status.instances.first().name)
    }

    @Test
    fun parseStatus_marksDetailedInstanceRunning_whenListInstancesConfirmsIt() {
        val status = EasyTierRuntime.parseStatus(
            raw = """{"map":{"instance-1":{"running":false,"peers":[],"routes":[],"my_node_info":{"hostname":"home"}}}}""",
            runningInstancesRaw = """{"home":"instance-1"}"""
        )

        assertEquals(1, status.instanceCount)
        assertEquals(1, status.runningCount)
        assertTrue(status.instances.first().running)
    }

    @Test
    fun parseStatus_mapsRelayNextHopPeerIdToDisplayFields() {
        val relayPeerId = "1234567890abcdef"
        val targetPeerId = "abcdefabcdef1234"
        val status = EasyTierRuntime.parseStatus(
            raw = """
                {
                  "map": {
                    "local-instance": {
                      "running": true,
                      "peers": [],
                      "routes": [],
                      "my_node_info": {
                        "hostname": "local",
                        "peer_id": "local-peer"
                      },
                      "peer_route_pairs": [
                        {
                          "route": {
                            "peer_id": "$relayPeerId",
                            "hostname": "relay-host",
                            "ipv4_addr": "10.144.0.2"
                          },
                          "peer": {
                            "peer_id": "$relayPeerId",
                            "conns": [
                              {
                                "conn_id": "relay-conn",
                                "is_closed": false,
                                "stats": {
                                  "latency_us": 1200
                                }
                              }
                            ]
                          }
                        },
                        {
                          "route": {
                            "peer_id": "$targetPeerId",
                            "hostname": "branch",
                            "ipv4_addr": "10.144.0.9",
                            "next_hop_peer_id": "$relayPeerId"
                          },
                          "peer": {
                            "peer_id": "$targetPeerId",
                            "conns": []
                          }
                        }
                      ]
                    },
                    "relay-instance-id": {
                      "dev_name": "relay-instance",
                      "running": true,
                      "peers": [],
                      "routes": [],
                      "my_node_info": {
                        "hostname": "relay-host",
                        "virtual_ipv4": "10.144.0.2",
                        "peer_id": "$relayPeerId"
                      }
                    }
                  }
                }
            """.trimIndent()
        )

        val target = status.peers.first { it.peerId == targetPeerId }

        assertEquals(relayPeerId, target.nextHopPeerId)
        assertEquals("relay-host", target.nextHopHostname)
        assertEquals("relay-instance", target.nextHopInstanceName)
        assertEquals("10.144.0.2", target.nextHopVirtualIp)
        assertEquals("12345678", target.nextHopShortPeerId)
        assertEquals(0, target.connectionCount)
    }

    @Test
    fun parseStatus_ignoresSelfNextHopPeerIdForDirectPeer() {
        val peerId = "abcdefabcdef1234"
        val status = EasyTierRuntime.parseStatus(
            raw = """
                {
                  "map": {
                    "local-instance": {
                      "running": true,
                      "peers": [],
                      "routes": [],
                      "peer_route_pairs": [
                        {
                          "route": {
                            "peer_id": "$peerId",
                            "hostname": "branch",
                            "ipv4_addr": "10.144.0.9",
                            "next_hop_peer_id": "$peerId"
                          },
                          "peer": {
                            "peer_id": "$peerId",
                            "conns": []
                          }
                        }
                      ]
                    }
                  }
                }
            """.trimIndent()
        )

        assertNull(status.peers.single().nextHopPeerId)
    }

    @Test
    fun parseStatus_readsPeerVirtualIpFromAlternateRouteField() {
        val peerId = "abcdefabcdef1234"
        val status = EasyTierRuntime.parseStatus(
            raw = """
                {
                  "map": {
                    "local-instance": {
                      "running": true,
                      "peers": [],
                      "routes": [],
                      "peer_route_pairs": [
                        {
                          "route": {
                            "peer_id": "$peerId",
                            "hostname": "branch",
                            "ipv4": "10.144.0.9",
                            "version": "2.6.2"
                          },
                          "peer": {
                            "peer_id": "$peerId",
                            "conns": []
                          }
                        }
                      ]
                    }
                  }
                }
            """.trimIndent()
        )

        assertEquals("10.144.0.9", status.peers.single().virtualIp)
    }

    @Test
    fun parseStatus_readsPeerVirtualIpFromProtobufIpv4Inet() {
        val peerId = "abcdefabcdef1234"
        val status = EasyTierRuntime.parseStatus(
            raw = """
                {
                  "map": {
                    "local-instance": {
                      "running": true,
                      "peers": [],
                      "routes": [],
                      "peer_route_pairs": [
                        {
                          "route": {
                            "peer_id": "$peerId",
                            "hostname": "branch",
                            "ipv4_addr": {
                              "address": { "addr": 177209353 },
                              "network_length": 24
                            }
                          },
                          "peer": {
                            "peer_id": "$peerId",
                            "conns": []
                          }
                        }
                      ]
                    }
                  }
                }
            """.trimIndent()
        )

        assertEquals("10.144.0.9", status.peers.single().virtualIp)
    }

    @Test
    fun parseStatus_readsInstanceVirtualIpFromProtobufIpv4Inet() {
        val status = EasyTierRuntime.parseStatus(
            raw = """
                {
                  "map": {
                    "local-instance": {
                      "running": true,
                      "peers": [],
                      "routes": [],
                      "my_node_info": {
                        "hostname": "local",
                        "virtual_ipv4": {
                          "address": { "addr": 177209346 },
                          "network_length": 24
                        }
                      }
                    }
                  }
                }
            """.trimIndent()
        )

        assertEquals("10.144.0.2", status.instances.single().virtualIp)
    }
}
