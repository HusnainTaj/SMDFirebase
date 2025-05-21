package com.smd.l226786.smdfirebase

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smd.l226786.smdfirebase.databinding.ActivityProfileBinding
import com.smd.l226786.smdfirebase.util.DepartmentMapping
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var selectedDepartmentId: Int = 0

    // regex pattern for student ID validation: XXL-XXXX where X can be a digit
    private val studentIdPattern = Pattern.compile("^\\d{2}L-\\d{4}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupDatePicker()
        setupDepartmentDropdown()

        binding.buttonUpdate.setOnClickListener {
            if (validateInputs()) {
                updateUserData()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        loadUserData()
    }

    private fun setupDatePicker() {
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
        
        binding.dropdownDepartment.setOnItemClickListener { _, _, position, _ ->
            val selectedDepartment = departments[position]
            selectedDepartmentId = DepartmentMapping.getDepartmentId(selectedDepartment)
        }
    }

    private fun loadUserData() {
        showProgress(true)
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.textViewEmail.text = "Email: ${currentUser.email}"
            
            val userId = currentUser.uid
            val userRef = database.getReference("students").child(userId)
            
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showProgress(false)
                    
                    if (snapshot.exists()) {
                        val studentId = snapshot.child("id").getValue(String::class.java) ?: ""
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val departmentId = snapshot.child("department").getValue(Long::class.java)?.toInt() ?: 0
                        val yearOfStudy = snapshot.child("year_of_study").getValue(String::class.java) ?: ""
                        val dateOfRegistration = snapshot.child("date_of_registration").getValue(String::class.java) ?: ""
                        
                        binding.editTextStudentId.setText(studentId)
                        binding.editTextName.setText(name)
                        
                        val departmentName = DepartmentMapping.getDepartmentName(departmentId)
                        binding.dropdownDepartment.setText(departmentName, false)
                        selectedDepartmentId = departmentId
                        
                        binding.editTextYearOfStudy.setText(yearOfStudy)
                        binding.editTextDateOfRegistration.setText(dateOfRegistration)

                        val date = dateFormat.parse(dateOfRegistration)
                        if (date != null) {
                            calendar.time = date
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    showProgress(false)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Failed to load user data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            showProgress(false)
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val studentId = binding.editTextStudentId.text.toString().trim()
        val name = binding.editTextName.text.toString().trim()
        val department = binding.dropdownDepartment.text.toString().trim()
        val yearOfStudy = binding.editTextYearOfStudy.text.toString().trim()
        
        var isValid = true
        
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

    private fun updateUserData() {
        val currentUser = auth.currentUser ?: return
        
        val studentId = binding.editTextStudentId.text.toString().trim()
        val name = binding.editTextName.text.toString().trim()
        val yearOfStudy = binding.editTextYearOfStudy.text.toString().trim()
        val dateOfRegistration = binding.editTextDateOfRegistration.text.toString()
        
        showProgress(true)
        
        val updatedData = hashMapOf(
            "id" to studentId,
            "name" to name,
            "department" to selectedDepartmentId,
            "year_of_study" to yearOfStudy,
            "date_of_registration" to dateOfRegistration
        )
        
        val userRef = database.getReference("students").child(currentUser.uid)
        userRef.updateChildren(updatedData as Map<String, Any>)
            .addOnCompleteListener { task ->
                showProgress(false)
                
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Profile updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to update profile: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
