package com.largeevent.management.model

import java.io.*

class VerificationResult @JvmOverloads constructor(
    @JvmField val type: VerificationResultType?,
    @JvmField val title: String?,
    @JvmField val description: String?,
    @JvmField val certificateInfo: CertificateInfo?,
    val isBindingRequired: Boolean = false,
    @JvmField val chipIdForBinding: String? = null,
    val isVehicle: Boolean = false
) : Serializable

