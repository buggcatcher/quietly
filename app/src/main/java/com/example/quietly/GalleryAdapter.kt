// GalleryAdapter.kt
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryAdapter(private val context: Context) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private var pictures: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_picture, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val picture = pictures[position]
        val imageUrl = "http://your-node-server-ip:3000/processed/$picture" // Adjust as per your server endpoint
        Glide.with(context)
            .load(imageUrl)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return pictures.size
    }

    fun setPictures(pictures: List<String>) {
        this.pictures = pictures
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}
