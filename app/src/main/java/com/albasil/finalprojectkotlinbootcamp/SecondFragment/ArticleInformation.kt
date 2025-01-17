package com.albasil.finalprojectkotlinbootcamp.SecondFragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.albasil.finalprojectkotlinbootcamp.Adapter.firestore
import com.albasil.finalprojectkotlinbootcamp.R
import com.albasil.finalprojectkotlinbootcamp.UI.AddArticleDirections
import com.albasil.finalprojectkotlinbootcamp.UI.TabBarFragmentDirections
import com.albasil.finalprojectkotlinbootcamp.ViewModels.AddArticleViewModel
import com.albasil.finalprojectkotlinbootcamp.ViewModels.ArticleInformationViewModel
import com.albasil.finalprojectkotlinbootcamp.data.Article
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_article_information.view.*
import java.io.File

class ArticleInformation : Fragment() {

    private lateinit var articleInformationViewModel: ArticleInformationViewModel

    val myID = FirebaseAuth.getInstance().currentUser?.uid

    private var likesCounter: Int = 0
    private val args by navArgs<ArticleInformationArgs>()
    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_article_information, container, false)

        articleInformationViewModel = ViewModelProvider(this).get(ArticleInformationViewModel::class.java)
        //------------------------------------------------------------------------
        view.titleArticleInfo_xml.text = " ${args.articleData.title.toString()}"
        view.userNameInfo_xml.text = " ${args.articleData.userName.toString()}"
        view.articleDateInfo_xml.text = " ${args.articleData.date.toString()}"
        view.articleCategoryInfo_xml.text = " ${args.articleData.category.toString()}"
        view.articleDescraptionInfo_xml.text = " ${args.articleData.description.toString()}"
        //------------------------------------------------------------------------




        likesCounter = args.articleData.like.toInt()

        if (args.articleData.articleImage.toString().isNotEmpty()) {
            getArtciclePhoto(args.articleData.articleImage.toString())

        }else{
            view.articlePhotoInfo_xml.visibility=View.GONE
        }

        //يتاكد اذا في المفضلة او لا
      articleInformationViewModel.checkIfFavorite(myID.toString(),args.articleData.articleID,view)

        //----------------------
        view.favoriteArticle_xml.setOnClickListener {
            articleInformationViewModel.udDateFavorite(myID.toString(),args.articleData.articleID.toString(),args.articleData.userId,view)
         //   upDateFavorite(args.articleData.articleImage.toString())

        }

        view.shearArticle_xml.setOnClickListener {
            shareArticle("${view.titleArticleInfo_xml.text}", "${view.articleDescraptionInfo_xml.text}")
        }


        view.commentButton.setOnClickListener {

            val articleID = ArticleInformationDirections.actionArticleInformationToComments(args.articleData.articleID.toString())
           findNavController().navigate(articleID)


        }

        view.userNameInfo_xml.setOnClickListener {

                if (args.articleData.userId.equals(myID.toString())){
                    findNavController().navigate(R.id.profile)
                }else{
                    val userInformation =
                        ArticleInformationDirections.actionArticleInformationToUserProfile(args.articleData.userId)
                   findNavController().navigate(userInformation)

                }
        }

        return view
    }

    //---------------Share Article---------------------------------------------------
    fun shareArticle(titleArticle: String, subjectArticle: String) {

        val shareArticle = Intent(Intent.ACTION_SEND)
        shareArticle.type = "text/plain"
        shareArticle.putExtra(Intent.EXTRA_TEXT, subjectArticle.toString())
        shareArticle.putExtra(Intent.EXTRA_TITLE, titleArticle.toString())
        shareArticle.putExtra(Intent.EXTRA_SUBJECT, subjectArticle)

        startActivity(shareArticle)

    }

    fun getArtciclePhoto(imagePath: String) {

        val imageName = "${imagePath.toString()}"

        val storageRef = FirebaseStorage.getInstance().reference
            .child("/imagesArticle/$imageName")

        val localFile = File.createTempFile("tempImage", "*")//jpg
        storageRef.getFile(localFile).addOnSuccessListener {

            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            view?.articlePhotoInfo_xml?.load(bitmap)

        }.addOnFailureListener {
            //   Toast.makeText(context,"Failed image ",Toast.LENGTH_SHORT).show()

        }
    }

}

