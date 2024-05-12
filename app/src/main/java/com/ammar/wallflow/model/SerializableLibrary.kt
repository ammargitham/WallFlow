package com.ammar.wallflow.model

import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Funding
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Organization
import com.mikepenz.aboutlibraries.entity.Scm
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SerializableLibrary(
    @SerialName("uniqueId") val uniqueId: String,
    @SerialName("artifactVersion") val artifactVersion: String?,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String?,
    @SerialName("website") val website: String?,
    @SerialName("developers") val developers: List<Developer>,
    @SerialName("organization") val organization: Organization?,
    @SerialName("scm") val scm: Scm?,
    @SerialName("licenses") val licenses: Set<License> = setOf(),
    @SerialName("funding") val funding: Set<Funding> = setOf(),
    @SerialName("tag") val tag: String? = null,
) {
    /**
     * defines the [uniqueId]:[artifactVersion] combined
     */
    val artifactId: String
        get() = "${uniqueId}:${artifactVersion ?: ""}"

    /**
     * Returns `true` in cases this artifact is assumed to be open source (e..g. [scm].url is provided)
     */
    val openSource: Boolean
        get() = scm?.url?.isNotBlank() == true

    fun toLibrary() = Library(
        uniqueId = uniqueId,
        artifactVersion = artifactVersion,
        name = name,
        description = description,
        website = website,
        developers = developers.toImmutableList(),
        organization = organization,
        scm = scm,
        licenses = licenses.toImmutableSet(),
        funding = funding.toImmutableSet(),
        tag = tag,
    )
}

fun Library.toSerializableLibrary() = SerializableLibrary(
    uniqueId = uniqueId,
    artifactVersion = artifactVersion,
    name = name,
    description = description,
    website = website,
    developers = developers.toList(),
    organization = organization,
    scm = scm,
    licenses = licenses.toSet(),
    funding = funding.toSet(),
    tag = tag,
)
