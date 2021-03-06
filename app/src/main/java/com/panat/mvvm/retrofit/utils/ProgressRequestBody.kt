package com.panat.mvvm.retrofit.utils

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ProgressRequestBody : RequestBody {

    private val mFile: File
    private val ignoreFirstNumberOfWriteToCalls: Int
    private lateinit var content: MediaType


    constructor(mFile: File, s: MediaType) : super() {
        this.mFile = mFile
        this.content = s
        ignoreFirstNumberOfWriteToCalls = 0
        this.content =  s
    }

    constructor(
        mFile: File,
        ignoreFirstNumberOfWriteToCalls: Int,
        content: String
    ) : super() {
        this.mFile = mFile
        this.ignoreFirstNumberOfWriteToCalls = ignoreFirstNumberOfWriteToCalls
    }


    private var numWriteToCalls = 0

    private val getProgressSubject: PublishSubject<Float> = PublishSubject.create()

    fun getProgressSubject(): Observable<Float> {
        return getProgressSubject
    }


    override fun contentType(): MediaType {
        return content
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return mFile.length()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        numWriteToCalls++

        val fileLength = mFile.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val `in` = FileInputStream(mFile)
        var uploaded: Long = 0

        `in`.use { `in` ->
            var read: Int
            var lastProgressPercentUpdate = 0.0f
            read = `in`.read(buffer)
            while (read != -1) {

                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                read = `in`.read(buffer)

                // when using HttpLoggingInterceptor it calls writeTo and passes data into a local buffer just for logging purposes.
                // the second call to write to is the progress we actually want to track
                if (numWriteToCalls > ignoreFirstNumberOfWriteToCalls) {
                    val progress = (uploaded.toFloat() / fileLength.toFloat()) * 100f
                    //prevent publishing too many updates, which slows upload, by checking if the upload has progressed by at least 1 percent
                    if (progress - lastProgressPercentUpdate > 1 || progress == 100f) {
                        // publish progress
                        getProgressSubject.onNext(progress)
                        lastProgressPercentUpdate = progress
                    }
                }
            }
        }
    }


    companion object {

        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}