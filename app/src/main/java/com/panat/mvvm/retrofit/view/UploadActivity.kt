package com.panat.mvvm.retrofit.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.Cursor
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.Observer
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.panat.mvvm.retrofit.base.BaseActivity
import com.panat.mvvm.retrofit.databinding.ActivityUploadBinding
import com.panat.mvvm.retrofit.di.provideUpload
import com.panat.mvvm.retrofit.extension.hide
import com.panat.mvvm.retrofit.extension.show
import com.panat.mvvm.retrofit.extension.toastLong
import com.panat.mvvm.retrofit.service.UploadService
import com.panat.mvvm.retrofit.utils.FileHelper
import com.panat.mvvm.retrofit.viewModel.UpLoadViewModel
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import org.koin.android.ext.android.inject


class UploadActivity : BaseActivity<ActivityUploadBinding>() {


    val viewModel: UpLoadViewModel by inject()
    private lateinit var retrofit: UploadService
    private val GALLERY_REQUEST_CODE = 101

    override fun initView() {
        bindView(ActivityUploadBinding.inflate(layoutInflater))
        retrofit = provideUpload()

        binding.btnUploadImg.setOnClickListener {
            checkPermission()
        }

        viewModel.success.observe(this, Observer {
            binding.determinateBar.hide()
            binding.percent.hide()
            binding.image.alpha = 1F
        })

        viewModel.process.observe(this, Observer {
            binding.determinateBar.progress = it.toInt()
            val percent = "$it%"
            binding.percent.text = percent
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            binding.image.alpha = 0.5F

            val uri = Matisse.obtainResult(data).first()
            val path = uri.path

            path?.let {
                if (it.contains("/video/")) {
                    println("Upload Video")
                    beforeUploadVideo(uri)
                }
                if (it.contains("/images/")) {
                    println("Upload Image")
                    beforeUploadImage(uri)
                }
            }
        }
    }

    private fun beforeUploadImage(data: Uri) {
        binding.image.setImageURI(data)
        binding.determinateBar.show()
        binding.percent.show()
        val fileHelper = FileHelper()
        val realPath = fileHelper.getPathFromURI(this, data)
        realPath?.let {
            viewModel.upload(it)
        }
    }

    private fun beforeUploadVideo(uri: Uri) {
        val path = getRealPathFromURI(this, uri)
        binding.determinateBar.visibility = View.VISIBLE
        binding.percent.visibility = View.VISIBLE
        val b1Map =
                ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
        //val bMap = ThumbnailUtils.createVideoThumbnail(file, Size(120, 120), signal)
        binding.image.setImageBitmap(b1Map)
        viewModel.upload(path)
    }

    private fun checkPermission() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?
                    ) {
                        toastLong("permission denied")
                        token?.continuePermissionRequest()
                    }

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report?.areAllPermissionsGranted()!!) {
                            pickFromGallery()
                        }
                    }
                })
                .check()
    }

    private fun pickFromGallery() {
        Matisse.from(this)
                .choose(MimeType.ofAll())
                .showSingleMediaType(true)
                .countable(true)
                .maxSelectable(1)
                .gridExpectedSize(400)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.45f)
                .imageEngine(GlideEngine())
                .forResult(GALLERY_REQUEST_CODE)
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String {
        var cursor: Cursor? = null
        try {
            val proJ = arrayOf(MediaStore.Video.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proJ, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        } finally {
            cursor?.close()
        }
    }
}
