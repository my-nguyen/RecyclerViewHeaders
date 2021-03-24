/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.databinding.ListItemSleepNightBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// two constants for the view types
private val ITEM_VIEW_TYPE_HEADER = 0
private val ITEM_VIEW_TYPE_ITEM = 1

/*class SleepNightAdapter(val clickListener: SleepNightListener)
    : ListAdapter<SleepNight, SleepNightAdapter.ViewHolder>(SleepNightDiffCallback()) {*/
// Instead of only supporting one type of ViewHolder, SleepNightAdapter needs to use any type of ViewHolder
class SleepNightAdapter(val clickListener: SleepNightListener)
    : ListAdapter<DataItem, RecyclerView.ViewHolder>(SleepNightDiffCallback()) {

    // define a CoroutineScope with Dispatchers.Default
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    /*override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!, clickListener)
    }*/
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                // Cast the object type returned by getItem() to DataItem.SleepNightItem
                val nightItem = getItem(position) as DataItem.SleepNightItem
                holder.bind(nightItem.sleepNight, clickListener)
            }
        }
    }

    /*override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }*/
    // test for and return the appropriate view holder for each item type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> TextViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> ViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType ${viewType}")
        }
    }

    // return the right header or item constant depending on the type of the current item
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
            else -> ITEM_VIEW_TYPE_ITEM
        }
    }

    // Instead of using submitList() (provided by the ListAdapter) to submit your list,
    // use this function to add a header and then submit the list
    fun addHeaderAndSubmitList(list: List<SleepNight>?) {
        // launch a coroutine in the adapterScope to manipulate the list
        adapterScope.launch {
            val items = when (list) {
                // if the passed in list is null, return just a header
                null -> listOf(DataItem.Header)
                // otherwise, attach the header to the head of the list, and then submit the list
                else -> listOf(DataItem.Header) + list.map { DataItem.SleepNightItem(it) }
            }

            // switch to the Dispatchers.Main context to submit the list
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    // This class inflates the textview.xml layout, and returns a TextViewHolder instance
    class TextViewHolder(view: View): RecyclerView.ViewHolder(view) {
        companion object {
            fun from(parent: ViewGroup): TextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.header, parent, false)
                return TextViewHolder(view)
            }
        }
    }

    class ViewHolder private constructor(val binding: ListItemSleepNightBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SleepNight, clickListener: SleepNightListener) {
            binding.sleep = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSleepNightBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

/*class SleepNightDiffCallback : DiffUtil.ItemCallback<SleepNight>() {
    override fun areItemsTheSame(oldItem: SleepNight, newItem: SleepNight): Boolean {
        return oldItem.nightId == newItem.nightId
    }

    override fun areContentsTheSame(oldItem: SleepNight, newItem: SleepNight): Boolean {
        return oldItem == newItem
    }
}*/
// Change the methods in SleepNightDiffCallback to use the new DataItem class instead of the SleepNight
class SleepNightDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}

class SleepNightListener(val clickListener: (sleepId: Long) -> Unit) {
    fun onClick(night: SleepNight) = clickListener(night.nightId)
}

sealed class DataItem {
    // When the adapter uses DiffUtil to determine whether and how an item has changed,
    // the DiffItemCallback needs to know the id of each item
    abstract val id: Long

    // a wrapper around a SleepNight
    data class SleepNightItem(val sleepNight: SleepNight): DataItem() {
        override val id = sleepNight.nightId
    }
    // to represent a header. Since a header has no actual data, declare it as an object
    object Header: DataItem() {
        override val id = Long.MIN_VALUE
    }
}