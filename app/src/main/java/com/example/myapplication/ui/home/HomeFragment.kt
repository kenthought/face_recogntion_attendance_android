package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ClassAdapter
import com.example.myapplication.classes.ClassItem
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var database : DatabaseReference
    private lateinit var classRecyclerView: RecyclerView
    private lateinit var classItem: ArrayList<ClassItem>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        classRecyclerView = binding.classList
        classRecyclerView.layoutManager = LinearLayoutManager(context)
        classRecyclerView.setHasFixedSize(true)

        classItem = arrayListOf<ClassItem>()
        getClass()

        return root
    }

    private fun getClass() {
        val user = Firebase.auth.currentUser
        database = FirebaseDatabase.getInstance().getReference("classes/" + user!!.uid)
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                classItem.clear()
                if (snapshot.exists()) {
                    for (classSnapshot in snapshot.children) {
                        val classes = classSnapshot.getValue(ClassItem::class.java)
                        classItem.add(classes!!)
                    }
                    val dataAdapter = ClassAdapter(classItem)
                    classRecyclerView.adapter = dataAdapter

                    classRecyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}