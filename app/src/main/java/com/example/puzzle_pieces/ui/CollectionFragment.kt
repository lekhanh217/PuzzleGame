package com.example.puzzle_pieces.ui

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.adapter.ADTLibraryImage
import com.example.puzzle_pieces.databinding.FragmentCollectionBinding
import com.example.puzzle_pieces.utils.HandleWithFile
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CollectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CollectionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentCollectionBinding
    private lateinit var destinationUri : Uri;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCollectionBinding.inflate(layoutInflater);
        val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val resultUri = UCrop.getOutput(result.data!!)
                    val intent  = Intent(requireContext(), GameSizeActivity::class.java);
                    intent.putExtra("localImagePath",resultUri!!.path);
                    // uri.path trả ve duong dan, not file://, con uri.toString tra ve file://...
                    intent.putExtra("source","croppedGalleryImage");
                    startActivity(intent);
                } else if (result.resultCode == UCrop.RESULT_ERROR) {
                    val error = UCrop.getError(result.data!!)
                    Log.e("UCrop", "❌ Lỗi khi cắt ảnh: $error")
                    HandleWithFile().deleteLibraryImage(requireContext(),destinationUri.path!!);
                } else {
                    Log.d("UCrop", "❌ Người dùng đã hủy");
                    HandleWithFile().deleteLibraryImage(requireContext(),destinationUri.path!!);
                }
            }
        val getContent = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            object : ActivityResultCallback<Uri?>{
                override fun onActivityResult(result: Uri?) {
                    result?.let { uri ->
                    //    Log.d("ImagePicker", "✅ Đã chọn ảnh: $uri")
                        lifecycleScope.launch(Dispatchers.IO){
                            destinationUri =
                                Uri.fromFile(File((context as App).cacheDir, "cropped_image${System.currentTimeMillis()}.jpg"))
                            val option = UCrop.Options();
                            val intent = UCrop.of(uri, destinationUri)
                                .withOptions(option)
                                .withAspectRatio(4f,3f) // Tỷ lệ 4:3
                                .withMaxResultSize(1000, 1000)
                                .getIntent(requireContext())
                            lifecycleScope.launch(Dispatchers.Main) {
                                cropLauncher.launch(intent)
                            }
                        }
                    } ?: Log.e("ImagePicker", "❌ Không chọn ảnh nào")
                }
            }
        )

        binding.ibnOpenGallery.setOnClickListener {
            getContent.launch("image/*");
        }
        binding.rcvLibraryImageList.adapter = ADTLibraryImage(this);
        binding.rcvLibraryImageList.layoutManager = GridLayoutManager(requireContext(),2);
        binding.rcvLibraryImageList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ){
                outRect.bottom = 5.dp;
                outRect.left =
                    3.dp;
                outRect.right = 3.dp;
            }
        })
        return binding.root
    }
    fun fetchImages(){
        lifecycleScope.launch(Dispatchers.IO){
            val newList = HandleWithFile().getLibraryImage(requireContext());
            lifecycleScope.launch(Dispatchers.Main){
                if(newList.size == 0){
                   binding.cslShowEmptyList.visibility = View.VISIBLE;
                }else {
                    binding.cslShowEmptyList.visibility = View.GONE;
                    (binding.rcvLibraryImageList.adapter as ADTLibraryImage).setNewImageList(newList);
                }
            }
        }
    }

    override fun onResume() {
        fetchImages()
        super.onResume()
    }
}