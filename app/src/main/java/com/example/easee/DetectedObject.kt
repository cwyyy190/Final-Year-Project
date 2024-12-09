package com.example.easee

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetectedObject(
    val label: String = "",
    val confidence: Double = 0.0,
    val timestamp: Long = 0L,
    var imageUrl: String = ""
)

class DetectedObjectsAdapter(
    private var detectedObjects: List<DetectedObject>
) : RecyclerView.Adapter<DetectedObjectsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivObjectImage)
        val label: TextView = itemView.findViewById(R.id.tvLabel)
        val confidence: TextView = itemView.findViewById(R.id.tvConfidence)
        val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detected_object, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = detectedObjects[position]

        val detectedObject = detectedObjects[position]
        holder.label.text = detectedObject.label
        holder.confidence.text = "Confidence: ${detectedObject.confidence}%"
        holder.timestamp.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(detectedObject.timestamp))

        Glide.with(holder.imageView.context)
            .load(detectedObject.imageUrl)
            .placeholder(R.drawable.img_placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = detectedObjects.size

    fun updateData(newData: List<DetectedObject>) {
        detectedObjects = newData
        notifyDataSetChanged()
    }
}
