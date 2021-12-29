package com.albasil.finalprojectkotlinbootcamp.UI

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog.show
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.albasil.finalprojectkotlinbootcamp.Adapter.ArticleUserProfileAdapter
import com.albasil.finalprojectkotlinbootcamp.R
import com.albasil.finalprojectkotlinbootcamp.ViewModels.ProfileViewModel
import com.albasil.finalprojectkotlinbootcamp.data.Article
import com.albasil.finalprojectkotlinbootcamp.databinding.FragmentProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.android.synthetic.main.item_article.*
import kotlinx.android.synthetic.main.item_article_user_profile.*
import kotlinx.android.synthetic.main.up_date_user_information.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class Profile : Fragment() {

    val myID = FirebaseAuth.getInstance().currentUser?.uid
    lateinit var binding: FragmentProfileBinding
    private lateinit var imageUrl: Uri
    private lateinit var userPhoneNumber: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var articleList: MutableList<Article>
    private lateinit var articleAdapter: ArticleUserProfileAdapter

    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //---------------------------------------------------
        getUserPhoto()
        getUserInfo(myID.toString())

        //---------------------------------------------------------

        recyclerView = view.findViewById(R.id.userProfileRecyclerView_xml)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.setHasFixedSize(true)
        articleList = mutableListOf()
        articleAdapter = ArticleUserProfileAdapter(articleList)
        recyclerView.adapter = articleAdapter

        //---------------------------------------------------------

        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        profileViewModel.getAllMyArticles(myID.toString(), articleList, viewLifecycleOwner)
            .observe(viewLifecycleOwner, {

                binding.userProfileRecyclerViewXml.adapter = ArticleUserProfileAdapter(articleList)
                binding.numberOrArticle.setText(articleList.size.toString())

                articleAdapter.notifyDataSetChanged()

            })

//       binding.userNameXml.text=  profileViewModel.getUserInformation(myID.toString()).userName


        //-----------------------------------------------------
        binding.tvFollowersXml.setOnClickListener {

            val check = ProfileDirections.actionProfileToFollowersUserFragment("Followers")
            NavHostFragment.findNavController(this).navigate(check)
        }
        binding.tvFollowingXml.setOnClickListener {
            val check = ProfileDirections.actionProfileToFollowersUserFragment("Following")
            NavHostFragment.findNavController(this).navigate(check)

        }



        binding.userImageProfileXml.setOnClickListener { selectImage() }

        binding.addInformationXml.setOnClickListener { showDialogUserInfo()
        }

        binding.upDateButtonXml.setOnClickListener {
            upDateUserInfoBottomSheet()
        }

        val deleteArticle = ItemTouchHelper(itemTouchHelper)

        deleteArticle.attachToRecyclerView(recyclerView)

    }


    // Swipe
    val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val article = articleList[position]
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    articleList.remove(article)
                    articleAdapter.notifyItemRemoved(position)


                    profileViewModel.deleteArticle(article.articleID)
                    view?.let {
                        Snackbar.make(it, "Article Deleted Successfully", Snackbar.LENGTH_LONG).apply {
                            setAction("Undo") {
                                profileViewModel.addArticle(article)
                            }
                            show()

                        }}
                    }
                ItemTouchHelper.RIGHT -> {
                    upDateUserArticle(article)
                }
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addSwipeLeftBackgroundColor(android.graphics.Color.parseColor("#0000FF"))
                .addSwipeLeftActionIcon(R.drawable.ic_delete_24)
                .create().decorate()
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun upDateUserArticle(article: Article) {
        val article_data = Article()
        article_data.title = article.title
        article_data.userName = article.userName
        article_data.date = article.date.toString()
        article_data.category = article.category
        article_data.description = article.description
        article_data.articleImage = article.articleImage
        article_data.articleID = article.articleID.toString()
        val itemData = ProfileDirections.actionProfileToEditArticle(article_data)

        NavHostFragment.findNavController(this@Profile).navigate(itemData)

    }


    fun showDialogUserInfo() {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("User Information")
        val view: View = layoutInflater.inflate(R.layout.add_user_information_dialog, null)
        val editTextExperience: EditText = view.findViewById(R.id.editTextAddExperience)
        builder.setView(view)
        editTextExperience.setText(binding.userInfoXml.text.toString())
        builder.setPositiveButton("Save") { _, _ ->

            profileViewModel.addUserInfo(myID.toString(), editTextExperience.text.toString())
            binding.userInfoXml.setText(editTextExperience.text.toString())

        }

        builder.setNegativeButton("Cancel", { _, _ -> })

        builder.show()
    }


    //احطها في الفيو موديل
    fun getUserInfo(userID: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("Users")
                .document("$userID")
                .get().addOnCompleteListener { it

                    if (it.result?.exists()!!) {
                        //+++++++++++++++++++++++++++++++++++++++++
                        val name = it.result!!.getString("userName")
                        val userFollowing = it.result!!.get("following")
                        val userFollowers = it.result!!.get("followers")
                        val userPhone = it.result!!.getString("userPhone")//moreInfo
                        val userInfo = it.result!!.getString("moreInfo")//moreInfo

                        binding.userNameXml.text = "${name.toString()}"
                        binding.userInfoXml.text = "${userInfo.toString()}"
                        binding.userFollowersXml.text = "${userFollowers?.toString()}"
                        binding.userFollowingXml.text = "${userFollowing?.toString()}"
                        userPhoneNumber = "${userPhone.toString()}"

                    } else {
                        Log.e("error \n", "errooooooorr")
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("FUNCTION createUserFirestore", "${e.message}")
            }
        }

    }


    //--------------------------------------------------------------------------------

    private fun upDateUserInfoBottomSheet() {
        val view: View = layoutInflater.inflate(R.layout.up_date_user_information, null)
        val builder = BottomSheetDialog(requireView().context as Context)
        builder.setTitle("Up Date Information")

        val upDateInfoButton = view.upDateInfoButton_xml
        view.editTextTextUserName.setText("${binding.userNameXml.text.toString()}")
        view.editTextPhone.setText("${userPhoneNumber.toString()}")
        upDateInfoButton.setOnClickListener {
            if (view.editTextTextUserName.text.toString().isNotEmpty() &&
                view.editTextPhone.text.toString().isNotEmpty() &&
                view.editTextPhone.text.toString().length == 10
            ) {

                binding.userNameXml.setText(view.editTextTextUserName.text.toString())
                profileViewModel.upDateUserInformation(
                    "${view.editTextTextUserName.text.toString()}",
                    "${view.editTextPhone.text.toString()}"
                )

                builder.dismiss()
            } else {
                Toast.makeText(context, "Please enter correct Information!!! ", Toast.LENGTH_SHORT)
                    .show()
            }

        }
        builder.setContentView(view)
        builder.show()
    }

    //--------------------------------------------------------------------------------------
    private fun selectImage() {

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {

            imageUrl = data?.data!!

            binding.userImageProfileXml.setImageURI(imageUrl)

            //*******************************************************

            upLoadImage()
        }

    }

    fun upLoadImage() {

        //-----------UID------------------------
        val uId = FirebaseAuth.getInstance().currentUser?.uid
//
//        val progressDialog = ProgressDialog(context)
//        progressDialog.setMessage("Uploading File ...")
//        progressDialog.setCancelable(false)
//
//        progressDialog.show()

        val storageReference = FirebaseStorage.getInstance().getReference("imagesUsers/${uId}")

        storageReference.putFile(imageUrl)
            .addOnSuccessListener {
                Toast.makeText(context, "uploading image", Toast.LENGTH_SHORT).show()

//                if (progressDialog.isShowing) progressDialog.dismiss()

                getUserPhoto()


            }.addOnFailureListener {
//                if (progressDialog.isShowing) progressDialog.dismiss()
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
            }
    }

    fun getUserPhoto() {

        val imageName = "${FirebaseAuth.getInstance().currentUser?.uid}"

        val storageRef = FirebaseStorage.getInstance().reference
            .child("imagesUsers/$imageName")

        val localFile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localFile).addOnSuccessListener {

            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

            binding.userImageProfileXml.load(bitmap)
            //  binding.userImageProfileXml.load(localFile)

        }.addOnFailureListener {
            Toast.makeText(context, "Failed image ", Toast.LENGTH_SHORT).show()
        }
    }
    //--------------------------------------------------------------------------------------


}
