package com.nsromapa.frenzapp.saytalk.models

import android.os.Parcelable
import com.nsromapa.frenzapp.saytalk.utils.FirebaseUtils
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

public class Models {

    public data class callModel(
        var call_id: String = "",
        var call_initiator: String = "",
        var call_involvers: String = "",
        var call_initiated_time: String = "",
        var call_status: String = "",
        var call_type: String = "",
        var call_target_type: String = "",
        var audio_only: Boolean = false
    )

    public data class MessageModel(
        var message: String = "",
        var from: String = "",
        var to: String = "",
        var timeInMillis: Long = System.currentTimeMillis(),
        var reverseTimeStamp: Long = timeInMillis * -1,
        var isFile: Boolean = false,
        var messageType: String = "message",
        var caption: String = "",
        var file_local_path: String = "",
        var file_size_in_bytes: Long = 0,
        var message_deleted: Boolean = false
    ) : Serializable

    @Parcelize
    public data class Contact(
        var name: String = "",
        var number: String = "",
        var photoURI: String = "",
        var uid: String = ""
    ) : Parcelable

    public data class User(
        var name: String = "",
        var createdOn: Long = System.currentTimeMillis(),
        var lastModifiedOn: Long = createdOn,
        var phone: String = "",
        var profile_pic_url: String = "",
        var uid: String = "",
        var country: String = "",
        var countryCode: String = "",
        var countryLocaleCode: String = "",
        var full_name: String = "",
        var bio: String = "",
        var city: String = ""
    )


    public data class LastMessageDetail(
        var timeInMillis: Long = System.currentTimeMillis(),
        var type: String = FirebaseUtils.KEY_CONVERSATION_SINGLE, // single or group
        var reverseTimeStamp: Long = timeInMillis * -1,
        var nameOrNumber: String = ""
    )


    public data class MessageStatus(
        var from: String = "",
        var read: Boolean = false,
        var delivered: Boolean = false,
        var messageID: String = "",
        var senderPhoneNumber: String = "",
        var senderPhotoURL: String = "",
        var groupNameIfGroup: String = ""
    )

    public data class UserActivityStatus(
        var status: String = "offline",
        var timeInMillis: Long = System.currentTimeMillis()
    )

    public data class File(
        var fileID: String = "",
        var uploadTime: Long = System.currentTimeMillis(),
        var uploadedBy: String = FirebaseUtils.getUid(),
        var reverseTimeStamp: Long = uploadTime * -1,
        var fileType: String = "",
        var fileSizeInBytes: Long = 0,
        var bucket_path: String = "",
        var file_url: String = "",
        var file_extension: String = ""
    )


    public data class Group(
        var name: String = "",
        var createdOn: Long = System.currentTimeMillis(),
        var createdBy: String = "",
        var groupID: String = "",
        var reverseTimeStamp: Long = createdOn * -1,
        var profile_pic_url: String = ""
    )


    public data class Channel(
        var name: String = "",
        var createdOn: Long = System.currentTimeMillis(),
        var createdBy: String = "",
        var channelID: String = "",
        var reverseTimeStamp: Long = createdOn * -1,
        var profile_pic_url: String = "",
        var channel_privacy: String = "public",
        var channel_url: String = ""
    )

    public data class GroupMember(
        var uid: String = "",
        var addedBy: String = "",
        var addedByPhoneNumber: String = "",
        var phoneNumber: String = "",
        var admin: Boolean = false,
        var removed: Boolean = false,
        var addedOn: Long = System.currentTimeMillis(),
        var reverseTimeStamp: Long = addedOn * -1
    )

    public data class ChannelMember(
        var uid: String = "",
        var addedBy: String = "",
        var addedByPhoneNumber: String = "",
        var phoneNumber: String = "",
        var admin: Boolean = false,
        var removed: Boolean = false,
        var addedOn: Long = System.currentTimeMillis(),
        var reverseTimeStamp: Long = addedOn * -1,
        var admin_power: Int = 0
    )


    public data class Feedback(
        var uid: String = "",
        var feedback: String = "",
        var addedOn: Long = System.currentTimeMillis(),
        var reverseTimeStamp: Long = addedOn * -1
    )

}