package com.smd.l226786.smdfirebase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smd.l226786.smdfirebase.adapter.StudentAdapter
import com.smd.l226786.smdfirebase.databinding.ActivityMainBinding
import com.smd.l226786.smdfirebase.model.Student
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var studentAdapter: StudentAdapter
    private var currentUserDepartment: Int = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        
        // Set up RecyclerView
        studentAdapter = StudentAdapter(emptyList(), currentUserDepartment)
        binding.recyclerViewStudents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = studentAdapter
        }
        
        loadData()
        
        binding.buttonLogout.setOnClickListener {
            auth.signOut()

            // clear shared preferences
            val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putBoolean("is_logged_in", false)
                remove("user_id")
                apply()
            }

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
        binding.buttonEditProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadData() {
        showLoading()

        binding.textViewWelcome.text = "Loading..."

        val studentsRef = database.getReference("students")
        val currentUserId = auth.currentUser?.uid ?: ""
        
        studentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val studentsList = mutableListOf<Student>()
                
                for (studentSnapshot in snapshot.children) {
                    val uid = studentSnapshot.key ?: ""
                    val id = studentSnapshot.child("id").getValue(String::class.java) ?: ""
                    val name = studentSnapshot.child("name").getValue(String::class.java) ?: ""
                    val departmentId = studentSnapshot.child("department").getValue(Long::class.java)?.toInt() ?: 0
                    val yearOfStudy = studentSnapshot.child("year_of_study").getValue(String::class.java) ?: ""
                    val dateOfRegistration = studentSnapshot.child("date_of_registration").getValue(String::class.java) ?: ""

                    // skip the current logged-in user
                    if (uid == currentUserId) {
                        binding.textViewWelcome.text = "Hello $name"
                        currentUserDepartment = departmentId

                        continue
                    }

                    val student = Student(uid, id, name, departmentId, yearOfStudy, dateOfRegistration)
                    studentsList.add(student)
                }
                
                // sort students by date of registration (ascending)
                val sortedStudents = studentsList.sortedBy { 
                    try {
                        dateFormat.parse(it.date_of_registration)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                studentAdapter = StudentAdapter(sortedStudents, currentUserDepartment)
                binding.recyclerViewStudents.adapter = studentAdapter
                
                hideLoading()
            }
            
            override fun onCancelled(error: DatabaseError) {
                hideLoading()
                
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load students: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    
    private fun showLoading() {
        binding.textViewLoading.visibility = View.VISIBLE
        binding.recyclerViewStudents.visibility = View.GONE
    }
    
    private fun hideLoading() {
        binding.textViewLoading.visibility = View.GONE
        binding.recyclerViewStudents.visibility = View.VISIBLE
    }
}
