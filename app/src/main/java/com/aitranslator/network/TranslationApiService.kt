package com.aitranslator.network

import com.aitranslator.data.model.TranslationRequest
import com.aitranslator.data.model.TranslationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for the LibreTranslate REST API.
 *
 * LibreTranslate docs: https://libretranslate.com/docs/
 *
 * To switch to a self-hosted instance, update LIBRE_TRANSLATE_BASE_URL in build.gradle.
 */
interface TranslationApiService {

    /**
     * Sends a POST request to /translate with the text and language codes.
     *
     * @param request [TranslationRequest] containing text, source code, target code, and API key.
     * @return Wrapped [TranslationResponse] with the translated text.
     */
    @POST("translate")
    suspend fun translate(
        @Body request: TranslationRequest
    ): Response<TranslationResponse>
}
