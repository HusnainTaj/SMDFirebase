package com.smd.l226786.smdfirebase

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smd.l226786.smdfirebase.databinding.ActivitySignUpBinding
import com.smd.l226786.smdfirebase.util.DepartmentMapping
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var selectedDepartmentId: Int = 0

    // regex pattern for student ID validation: XXL-XXXX where X can be a digit
    private val studentIdPattern = Pattern.compile("^\\d{2}[lL]-\\d{4}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupDatePicker()
        setupDepartmentDropdown()

        binding.buttonSignUp.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        binding.textViewLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        binding.editTextDateOfRegistration.setText(dateFormat.format(calendar.time))
        
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            binding.editTextDateOfRegistration.setText(dateFormat.format(calendar.time))
        }

        binding.editTextDateOfRegistration.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupDepartmentDropdown() {
        val departments = DepartmentMapping.getDepartmentNames()
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        binding.dropdownDepartment.setAdapter(adapter)
        
        if (departments.isNotEmpty()) {
            binding.dropdownDepartment.setText(departments[0], false)
            selectedDepartmentId = DepartmentMapping.getDepartmentId(departments[0])
        }
        
        binding.dropdownDepartment.setOnItemClickListener { _, _, position, _ ->
            val selectedDepartment = departments[position]
            selectedDepartmentId = DepartmentMapping.getDepartmentId(selectedDepartment)
        }
    }


    private fun validateInputs(): Boolean {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val studentId = binding.editTextStudentId.text.toString().trim()
        val name = binding.editTextName.text.toString().trim()
        val department = binding.dropdownDepartment.text.toString().trim()
        val yearOfStudy = binding.editTextYearOfStudy.text.toString().trim()
        
        var isValid = true

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Email is required"
            isValid = false
        } else {
            binding.textInputLayoutEmail.error = null
        }
        
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.textInputLayoutPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.textInputLayoutPassword.error = null
        }
        
        if (studentId.isEmpty()) {
            binding.textInputLayoutStudentId.error = "Student ID is required"
            isValid = false
        } else if (!studentIdPattern.matcher(studentId).matches()) {
            binding.textInputLayoutStudentId.error = "Student ID must be in format XXL-XXXX (e.g., 22L-1234)"
            isValid = false
        } else {
            binding.textInputLayoutStudentId.error = null
        }
        
        if (name.isEmpty()) {
            binding.textInputLayoutName.error = "Name is required"
            isValid = false
        } else {
            binding.textInputLayoutName.error = null
        }
        
        if (department.isEmpty()) {
            binding.textInputLayoutDepartment.error = "Department is required"
            isValid = false
        } else if (selectedDepartmentId == 0) {
            binding.textInputLayoutDepartment.error = "Please select a valid department"
            isValid = false
        } else {
            binding.textInputLayoutDepartment.error = null
        }
        
        if (yearOfStudy.isEmpty()) {
            binding.textInputLayoutYearOfStudy.error = "Year of study is required"
            isValid = false
        } else {
            binding.textInputLayoutYearOfStudy.error = null
        }

        return isValid
    }

    private fun registerUser() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val studentId = binding.editTextStudentId.text.toString().trim()
        
        showProgress(true)
        
        checkStudentIdUnique(studentId) { isUnique ->
            if (isUnique) {
                createFirebaseUser(email, password)
            } else {
                showProgress(false)
                binding.textInputLayoutStudentId.error = "$studentId already exists"
            }
        }
    }
    
    private fun checkStudentIdUnique(studentId: String, callback: (Boolean) -> Unit) {
        val studentsRef = database.getReference("students")
        
        studentsRef.orderByChild("id").equalTo(studentId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(!snapshot.exists())
            }
            
            override fun onCancelled(error: DatabaseError) {
                showProgress(false)
                Toast.makeText(
                    this@SignUpActivity,
                    "Error checking student ID: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                callback(false)
            }
        })
    }
    
    private fun createFirebaseUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    
                    if (userId != null) {
                        saveStudentData(userId)
                    } else {
                        showProgress(false)
                        Toast.makeText(
                            this,
                            "Failed to get user ID. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    showProgress(false)
                    
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(
                            this,
                            "Email $email already exists. Please login.",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Sign up failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun saveStudentData(userId: String) {
        val studentId = binding.editTextStudentId.text.toString().trim()
        val name = binding.editTextName.text.toString().trim()
        val yearOfStudy = binding.editTextYearOfStudy.text.toString().trim()
        val dateOfRegistration = binding.editTextDateOfRegistration.text.toString()
        
        val studentData = hashMapOf(
            "id" to studentId,
            "name" to name,
            "department" to selectedDepartmentId,
            "year_of_study" to yearOfStudy,
            "date_of_registration" to dateOfRegistration
        )
        
        val studentsRef = database.getReference("students")
        studentsRef.child(userId).setValue(studentData)
            .addOnCompleteListener { task ->
                showProgress(false)
                
                if (task.isSuccessful) {
                    saveUserLoggedIn(userId)

                    Toast.makeText(
                        this,
                        "Sign up successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to save user data: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    auth.currentUser?.delete()
                }
            }
    }

    private fun saveUserLoggedIn(userId: String) {
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", userId)
            apply()
        }
    }


    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
