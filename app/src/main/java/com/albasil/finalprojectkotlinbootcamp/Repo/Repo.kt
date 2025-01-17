package com.albasil.finalprojectkotlinbootcamp.Repo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.albasil.finalprojectkotlinbootcamp.Adapter.firestore
import com.albasil.finalprojectkotlinbootcamp.data.Users
import com.albasil.finalprojectkotlinbootcamp.Firebase.FirebaseAuthentication
import com.albasil.finalprojectkotlinbootcamp.R
import com.albasil.finalprojectkotlinbootcamp.data.Article
import com.albasil.finalprojectkotlinbootcamp.data.Comment
import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.change_password_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_article_information.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@SuppressLint("StaticFieldLeak")
val fireStore :FirebaseFirestore= FirebaseFirestore.getInstance()
 val imageRef = Firebase.storage.reference

class AppRepo(val context: Context) {


    private var imageUrl: Uri? = null

    val firebase = FirebaseAuthentication()
    val myID = FirebaseAuth.getInstance().currentUser?.uid


    suspend fun insertUserToDB(users: Users) {


    }


    suspend fun addArticleToFirestore(article: Article, view: View) = CoroutineScope(Dispatchers.IO).launch {
        try {
            firestore.collection("Articles").document(article.articleID).set(article)
                .addOnCompleteListener {it
                when {
                    it.isSuccessful -> {
//                        upLoadImage("${article.articleImage.toString()}")


                        Log.e("Add Article", "Done ${article.title}")
                    }
                    else -> {
                        Log.e("Field Article", "Error ${article.title}")
                    }
                }
            }
//            withContext(Dispatchers.Main) { }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("FUNCTION createUserFirestore", "${e.message}")
            }
        }
    }




    fun getUserArticles(userID: String, articleList: MutableList<Article>): LiveData<MutableList<Article>> {
        val article = MutableLiveData<MutableList<Article>>()
        fireStore.collection("Articles").whereEqualTo("userId", userID)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("Firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            articleList.add(dc.document.toObject(Article::class.java))
                        }
                    }
                    article.value = articleList
                }
            })
        return article
    }


    fun addUserInformation(myID: String, userInformation: String) =
        CoroutineScope(Dispatchers.IO).launch {
            val userInfo = hashMapOf("moreInfo" to "${userInformation}",)
            val userRef = Firebase.firestore.collection("Users")
            userRef.document("$myID").set(userInfo, SetOptions.merge()).addOnCompleteListener { it
                when {
                    it.isSuccessful -> {
                    }
                    else -> {
                    }
                }
            }
        }


    fun upDateUserInfo(upDateName: String, upDatePhoneNumber: String) {
        val uId = FirebaseAuth.getInstance().currentUser?.uid
        val upDateUserData = Firebase.firestore.collection("Users")
        upDateUserData.document(uId.toString()).update(
            "userName", "${upDateName.toString()}", "userPhone", "${upDatePhoneNumber.toString()}"
        )

    }




    fun getUserInfo(userID: String,userInfo :Users): LiveData<Users>{
        val user = MutableLiveData<Users>()
        fireStore.collection("Users").document("$userID")
            .get().addOnCompleteListener { it
                if (it.result?.exists()!!) {
                    userInfo.userName = it.result!!.getString("userName").toString()
                    userInfo.userPhone = it.result!!.getString("userPhone").toString()
                    userInfo.userEmail = it.result!!.getString("userEmail").toString()
                    userInfo.moreInfo =it.result!!.getString("moreInfo").toString()
                } else {
                }
                user.value= userInfo
            }
        return user
    }

    fun deleteArticle(articleID:String){
        val deleteArticle = Firebase.firestore.collection("Articles")
            .document(articleID).delete()
        deleteArticle.addOnCompleteListener {
            when {
                it.isSuccessful -> {
                    Log.d("Delete", "Delete Article")
                }
            }
        }
    }


    //-------------------------Settings----------------------------------
     fun changePassword(oldPassword: String, newPassword: String,confirmNewPassword: String,view:View) {

        if (oldPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmNewPassword.toString().isNotEmpty()) { Log.e("new password", "${newPassword.toString()}")
            if (newPassword.equals(confirmNewPassword)) {
                val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
                val userEmail = FirebaseAuth.getInstance().currentUser!!.email

                if (user.toString() != null && userEmail.toString() != null) {
                    val credential: AuthCredential = EmailAuthProvider.getCredential("${user?.email.toString()!!}",
                        "${oldPassword}")
                    user?.reauthenticate(credential)

                        ?.addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(view.context, "Auth Successful ", Toast.LENGTH_SHORT)
                                    .show()
                                //احط متغير
                                user.updatePassword("${newPassword.toString()}")
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(view.context, "isSuccessful , Update password", Toast.LENGTH_SHORT).show()

                                            view.etOldPassword_xml?.setText("")
                                            view.etConfirmNewPassword_xml?.setText("")
                                            view.etNewPassword_xml?.setText("")

                                        }
                                    }

                            } else {

                                Toast.makeText( view.context, " Failed Change Password ", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                else {
                }
            } else {
            }
        } else { }

    }


    //------------------------------------------------------------------------

    fun favoriteArticles(myID: String, articleList: MutableList<Article>
    ): LiveData<MutableList<Article>> {
        val article = MutableLiveData<MutableList<Article>>()
        fireStore.collection("Users").document(myID).collection("Favorite")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            articleList.add(dc.document.toObject(Article::class.java))
                        }
                    }
                    article.value = articleList
                }

            })
        return article
    }

    //--------------------editArticleData--------------------------------------------------------
    fun editArticleData(articleID:String,titleArticle:String,descraptaionArticle:String,category:String,imageArticleID: String,view: View){

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formatted = current.format(formatter)
        fireStore.collection("Articles").document("${articleID}")
            .update("title",titleArticle,"description",descraptaionArticle,
                "date",formatted,"category",category,"articleImage",imageArticleID).addOnCompleteListener { it
                when {
                    it.isSuccessful -> {
                         Toast.makeText(view.context,"UpDate ",Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                    }
                }
            }
    }

    //--------------------Article Favorite--------------------------------------------------------------------
     fun checkIfFavorite(myID: String,articleID: String,view:View) {
        firestore.collection("Users").document(myID)
            .collection("Favorite").document(articleID).get()
            .addOnCompleteListener {
                if (it.result?.exists()!!) {
                    view.favoriteArticle_xml?.setImageResource(R.drawable.ic_baseline_favorite_24)
                } else {
                    view.favoriteArticle_xml?.setImageResource(R.drawable.ic_favorite_border)
                }
            }
    }
    //---------------------------------------
     fun upDateFavorite(myID:String,articleID: String,userID: String,view: View) {
        //check in the fireStore
        fireStore.collection("Users").document(myID)
            .collection("Favorite").document(articleID).get()
            .addOnCompleteListener {
                if (it.result?.exists()!!) {

                    deleteFavorite(myID,articleID)
                    view.favoriteArticle_xml?.setImageResource(R.drawable.ic_favorite_border)

                } else {

                    addFavorite(articleID,userID)
                    view.favoriteArticle_xml?.setImageResource(R.drawable.ic_baseline_favorite_24)


                }
            }
    }

    //---------deleteFavorite------------------------------------------------------------------------------------------
    fun deleteFavorite(myID: String,articleID: String) {
        fireStore.collection("Articles").document(articleID)
            .collection("Favorite").document("${myID.toString()}").delete()
            .addOnCompleteListener {
                when {
                    it.isSuccessful -> {
                        Log.e("Delete Article ", "Delete From Articles Favorite")
                    }
                }
            }

        //-------------deleteFavoriteArticleUser----------------------------------------------------------
        fireStore.collection("Users").document(myID.toString())
            .collection("Favorite").document("${articleID.toString()}").delete()
            .addOnCompleteListener {
                when {
                    it.isSuccessful -> {
                        Log.e("Delete Article ", "Delete From User Favorite")
                    }
                }
            }
        numberOfFavorite(articleID)
    }

    //---------------addFavorite-------------------------------------------------------------------------------------------
    private fun addFavorite(articleID: String, userID:String) {
        val addFavorite = hashMapOf(
            "articleID" to articleID,
            "userId" to userID,
        )
        //---------------------------------------------------------------------------------
      firestore.collection("Users").document(myID.toString()).collection("Favorite")
            .document("${articleID.toString()}")
            .set(addFavorite).addOnCompleteListener {it
                when {
                    it.isSuccessful -> {
                        Log.d("Add Article", "Done to add User Favorite")
                    }
                    else -> {
                        Log.d("Error", "is not Successful fire store")
                    }
                }

                //---------------------------------------------------------------------------------
                val addToArticle = Firebase.firestore.collection("Articles")
                addToArticle.document(articleID.toString()).collection("Favorite")
                    .document("${myID.toString()}").set(addFavorite)
                //---------------------------------------------------------------------------------

                //delete ...
                numberOfFavorite(articleID)

            }
    }

    private fun numberOfFavorite(articleID: String) {
        firestore.collection("Articles").document(articleID)
            .collection("Favorite").get()
            .addOnSuccessListener {
                val numberOfFavorite = it.size()
                val userRef = Firebase.firestore.collection("Articles")
                userRef.document("$articleID").update("like", numberOfFavorite)

            }
    }



//-----------------Profile--------------------------------------
    fun getUserPhoto(): File {

        val imageName = "${FirebaseAuth.getInstance().currentUser?.uid}"

        val storageRef = FirebaseStorage.getInstance().reference
            .child("imagesUsers/$imageName")

        val localFile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localFile).addOnSuccessListener {

            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

//            binding.userImageProfileXml.load(bitmap)
            //  binding.userImageProfileXml.load(localFile)

        }.addOnFailureListener {
            Toast.makeText(context, "Failed image ", Toast.LENGTH_SHORT).show()
        }
        return localFile
    }



    //-----------------Home Page------------------------------------------------------------

    //----------------------getAllMyArticles-----------------------------------
    fun getAllArticles(articleList: MutableList<Article>): LiveData<MutableList<Article>> {
        val article = MutableLiveData<MutableList<Article>>()
        fireStore.collection("Articles").orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("Firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            articleList.add(dc.document.toObject(Article::class.java))
                        }
                    }
                    article.value = articleList
                }
            })

        return article
    }


    fun removeAllArticles(articleList: MutableList<Article>): LiveData<MutableList<Article>> {
        val article = MutableLiveData<MutableList<Article>>()
        fireStore.collection("Articles").orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("Firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            articleList.remove(dc.document.toObject(Article::class.java))
                        }
                    }
                    article.value = articleList
                }
            })

        return article
    }


    fun articleCategory(typeCategory:String,articleList: MutableList<Article>): LiveData<MutableList<Article>> {
        val article = MutableLiveData<MutableList<Article>>()
        fireStore.collection("Articles").whereEqualTo("category", typeCategory.toString())
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("Firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            articleList.add(dc.document.toObject(Article::class.java))
                        }
                    }
                    article.value = articleList
                }
            })

        return article
    }






    //-----------------------------------------------------------------------------------------------------------------------
    //-----------------------get Followers And Following------------------------------------------------------------------------
    fun getFollowersAndFollowing(type:String, articleList: MutableList<Users>
    ): LiveData<MutableList<Users>> {
        val article = MutableLiveData<MutableList<Users>>()

        fireStore.collection("Users").document(myID.toString())
            .collection(type.toString())
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("Firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            articleList.add(dc.document.toObject(Users::class.java))
                        }
                    }
                    article.value = articleList

                }

            })
        return article
    }


        //---------------------------User Profile--------------------------------------------------------------------------

    //-------------------Add------------------------
    fun addFollowers(myId: String, userId: String) {
        val upDateFollowers = hashMapOf(
            "userId" to "${myId}",
        )
        fireStore.collection("Users").document("${userId}")
            .collection("Followers").document("${myId}").set(upDateFollowers)

    }
    fun addFollowing(myId: String, userId: String) {
        val upDateFollowing = hashMapOf("userId" to "${userId}")
        fireStore.collection("Users").document("${myId}")
            .collection("Following").document("${userId}").set(upDateFollowing)

    }


    //----------------Delete---------------------------------------------------------
    fun deleteFollowers(myId: String, userId: String) {
        fireStore.collection("Users").document("${userId}")
            .collection("Followers").document("${myId}").delete()
    }
    fun deleteFollowing(myId: String, userId: String) {
        fireStore.collection("Users").document("${myId}")
            .collection("Following").document("${userId}").delete()
    }



    ///---------------------------------
    fun uploadArticleImage(image: Uri, fileName: String) =
        imageRef.child("imagesArticle/$fileName").putFile(image)





    ///---------------------------------
    fun uploadUserImage(image: Uri, userID: String) =
        imageRef.child("imagesUsers/$userID").putFile(image)

    suspend fun addComments(comment:Comment, view: View)= CoroutineScope(Dispatchers.IO).launch {
        try {
            firestore.collection("Articles").document(comment.articleID.toString()).
            collection("Comments").add(comment)
                .addOnCompleteListener {it
                    when {
                        it.isSuccessful -> {
                            Log.e("Add Article", "Done ${comment.dateFormat}")
                        }
                        else -> {
                            Log.e("Field Article", "Error ${comment.userID}")
                        }}}
        }catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("FUNCTION createUserFirestore", "${e.message}")
            }

    }
    }

    //----------------------getAllMyArticles-----------------------------------
    fun getAllComments(articleID: String,commentList: MutableList<Comment>): LiveData<MutableList<Comment>> {
        val comment = MutableLiveData<MutableList<Comment>>()
        fireStore.collection("Articles").document(articleID)
            .collection("Comments").orderBy("dateFormat", Query.Direction.ASCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("Firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            commentList.add(dc.document.toObject(Comment::class.java))
                        }
                    }
                    comment.value = commentList
                }
            })

        return comment
    }




}

