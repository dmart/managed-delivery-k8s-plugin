package com.amazon.spinnaker.keel.k8s.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.keel.retrofit.InstrumentedJacksonConverter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path


interface DockerV2Service {
    @GET("/v2/{image}/manifests/{reference}")
    @Headers(
        "Docker-Distribution-API-Version: registry/2.0",
        "Accept: application/vnd.docker.distribution.manifest.v2+json"
    )
    suspend fun getDigest(
        @Path("image") image: String,
        @Path("reference") reference: String
    ): DockerReference


    @GET("/v2/{image}/blobs/{digest}")
    @Headers(
        "Docker-Distribution-API-Version: registry/2.0",
    )
    suspend fun getDigestContent(
        @Path("image") image: String,
        @Path("digest") digest: String
    ): DigestContent
}

class DockerV2(
    objectMapper: ObjectMapper,
): DockerV2Service {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request()
                    .newBuilder()
                    .header("Authorization", "<token>") //FIXME
                    .build()
            )
        }
        .build()
    private val client = Retrofit.Builder()
        .addConverterFactory(InstrumentedJacksonConverter.Factory("dockerv2", objectMapper))
        .baseUrl("<baseUrl>") //FIXME
        .client(okHttpClient)
        .build()
        .create(DockerV2Service::class.java)

    override suspend fun getDigest(image: String, reference: String): DockerReference {
        return client.getDigest(image, reference)
    }

    override suspend fun getDigestContent(image: String, digest: String): DigestContent {
        return client.getDigestContent(image, digest)
    }
}


data class DigestContent(
    val config: DigestConfig
)

data class DigestConfig(
    val Env: List<String>,
    val Labels: Map<String, String>
)

data class DockerReference(
    val config: ReferenceConfig
)

data class ReferenceConfig(
    val digest: String
)
