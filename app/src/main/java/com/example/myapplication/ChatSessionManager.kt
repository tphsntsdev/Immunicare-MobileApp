package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class ChatSessionManager private constructor(context: Context) {
    private val prefkey = context.getString(R.string.prefKey)
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefkey, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()


    fun saveMessages(messages: List<chatbot_data>) {
        val gson = Gson()
        val json = gson.toJson(messages)
        editor.putString("chatMessages", json)
        editor.apply()
    }

    fun getMessages(): List<chatbot_data> {
        val gson = Gson()
        val json = sharedPreferences.getString("chatMessages", "")
        val type = object : TypeToken<List<chatbot_data>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }
    fun clearChatSession() {
        val mutableMessages = mutableListOf<chatbot_data>()
        mutableMessages.clear()
        saveMessages(mutableMessages)
    }
    fun resetMessages(messages: List<chatbot_data>) {
        saveMessages(messages)
        clearChatSession()
        chatbot_data(
            "Hello! How can I assist you today?, use Keywords such as  \"Account\", \"Vaccines\",\"Locations\",\"About Us\"",
            false
        )

    }
    companion object {
        @Volatile
        private var instance: ChatSessionManager? = null
        fun getInstance(context: Context): ChatSessionManager =
            instance ?: synchronized(this) {
                instance ?: ChatSessionManager(context).also { instance = it }
            }
    }

}