package com.flowfoundation.wallet.manager.flow

import com.nftco.flow.sdk.AddressRegistry
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import org.onflow.flow.ChainId
import org.onflow.flow.infrastructure.Cadence


class CadenceScriptBuilder {
    private var addressRegistry: AddressRegistry = FlowCadenceApi.DEFAULT_ADDRESS_REGISTRY
    private var _chainId: ChainId = FlowCadenceApi.DEFAULT_CHAIN_ID
    private var _script: String? = null
    private var _arguments: MutableList<Cadence.Value> = mutableListOf()

    var script: String
        get() { return _script!! }
        set(value) { _script = value }

    fun script(script: String) {
        this.script = script
    }

    fun script(script: String, chainId: ChainId = _chainId, addresses: Map<String, FlowAddress> = mapOf()) = script(
        addressRegistry.processScript(
            script = script,
            chainId = getFlowChainId(chainId),
            addresses = addresses
        )
    )

    fun script(chainId: ChainId = _chainId, addresses: Map<String, FlowAddress> = mapOf(), code: () -> String) = this.script(code(), chainId, addresses)

    private fun getFlowChainId(chainId: ChainId): FlowChainId {
        return when (chainId) {
            ChainId.Testnet -> FlowChainId.TESTNET
            else -> FlowChainId.MAINNET
        }
    }

    var arguments: MutableList<Cadence.Value>
        get() { return _arguments }
        set(value) {
            _arguments.clear()
            _arguments.addAll(value)
        }

    fun arguments(arguments: MutableList<Cadence.Value>) {
        this.arguments = arguments
    }

    fun arg(argument: () -> Cadence.Value) = _arguments.add(argument())
}