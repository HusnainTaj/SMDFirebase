package com.smd.l226786.smdfirebase.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smd.l226786.smdfirebase.R
import com.smd.l226786.smdfirebase.databinding.ItemStudentBinding
import com.smd.l226786.smdfirebase.model.Student
import com.smd.l226786.smdfirebase.util.DepartmentMapping

class StudentAdapter(
    private var students: List<Student> = emptyList(),
    private val currentUserDepartment: Int
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(val binding: ItemStudentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        val context = holder.itemView.context
        
        with(holder.binding) {
            textViewStudentName.text = student.name
            textViewStudentId.text = student.id
            textViewDepartment.text = DepartmentMapping.getDepartmentName(student.department)
            textViewRegistrationDate.text = student.date_of_registration
            
            if (student.department == currentUserDepartment) {
                cardViewStudent.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.same_department_highlight)
                )
            } else {
                cardViewStudent.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.black)
                )
            }
        }
    }

    override fun getItemCount(): Int = students.size
}
