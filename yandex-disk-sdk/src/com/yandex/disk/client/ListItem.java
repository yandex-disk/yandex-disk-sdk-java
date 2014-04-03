/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;
import android.util.Log;

public class ListItem implements Parcelable {

    private static final String TAG = "ListItem";

    private String displayName, fullPath, etag, contentType, ownerName, publicUrl, mediaType, mpfsFileId;
    private boolean isCollection, aliasEnabled, shared, readOnly, visible, hasThumbnail;
    private long contentLength, lastUpdated, etime;

    public static final class Builder {
        private String fullPath, displayName, lastModified, etag, contentType, ownerName, publicUrl, mediaType, mpfsFileId;
        private long contentLength, etime;
        private boolean isCollection, aliasEnabled, visible, shared, readOnly, hasThumbnail;

        public void setFullPath(String fullPath) {
            this.fullPath = fullPath;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setContentLength(long contentLength) {
            this.contentLength = contentLength;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public void addCollection() {
            isCollection = true;
        }

        public void setAliasEnabled(boolean aliasEnabled) {
            this.aliasEnabled = aliasEnabled;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void setShared(boolean shared) {
            this.shared = shared;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }

        public void setEtime(long etime) {
            this.etime = etime;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public void setMpfsFileId(String mpfsFileId) {
            this.mpfsFileId = mpfsFileId;
        }

        public void setHasThumbnail(boolean hasThumbnail) {
            this.hasThumbnail = hasThumbnail;
        }

        public ListItem build() {
            return new ListItem(fullPath, displayName, contentLength, lastModified, isCollection, etag,
                                contentType, shared, ownerName, aliasEnabled, readOnly, visible, publicUrl,
                                etime, mediaType, mpfsFileId, hasThumbnail);
        }
    }

    private ListItem(String fullPath, String displayName, long contentLength, String lastUpdated, boolean isCollection, String etag,
                     String contentType, boolean shared, String ownerName, boolean aliasEnabled, boolean readOnly, boolean visible,
                     String publicUrl, long etime, String mediaType, String mpfsFileId, boolean hasThumbnail) {
        this.fullPath = fullPath;
        if (displayName != null) {
            this.displayName = displayName;
        } else {
            this.displayName = new File(fullPath).getName();
        }
        this.contentLength = contentLength;
        this.lastUpdated = parseDateTime(lastUpdated);
        this.isCollection = isCollection;
        this.etag = etag;
        this.contentType = contentType;
        this.shared = shared;
        this.ownerName = ownerName;
        this.aliasEnabled = aliasEnabled;
        this.readOnly = readOnly;
        this.visible = visible;
        this.publicUrl = publicUrl;
        this.etime = etime;
        this.mediaType = mediaType;
        this.mpfsFileId = mpfsFileId;
        this.hasThumbnail = hasThumbnail;
    }

    private ListItem(String fullPath, String displayName, long contentLength, long lastUpdated, boolean isCollection, String etag,
                     boolean aliasEnabled, String contentType, boolean shared, boolean readonly, String ownerName, String publicUrl,
                     long etime, String mediaType, String mpfsFileId, boolean hasThumbnail) {
        this.fullPath = fullPath;
        this.displayName = displayName;
        this.contentLength = contentLength;
        this.lastUpdated = lastUpdated;
        this.isCollection = isCollection;
        this.etag = etag;
        this.aliasEnabled = aliasEnabled;
        this.contentType = contentType;
        this.shared = shared;
        this.readOnly = readonly;
        this.ownerName = ownerName;
        this.publicUrl = publicUrl;
        this.etime = etime;
        this.mediaType = mediaType;
        this.mpfsFileId = mpfsFileId;
        this.hasThumbnail = hasThumbnail;
    }

    private static final Map<String, Integer> MONTH = new HashMap<String, Integer>();

    static {
        MONTH.put("Jan", 0);
        MONTH.put("Feb", 1);
        MONTH.put("Mar", 2);
        MONTH.put("Apr", 3);
        MONTH.put("May", 4);
        MONTH.put("Jun", 5);
        MONTH.put("Jul", 6);
        MONTH.put("Aug", 7);
        MONTH.put("Sep", 8);
        MONTH.put("Oct", 9);
        MONTH.put("Nov", 10);
        MONTH.put("Dec", 11);
    }

    private static long parseDateTime(String datetime) {
        try {
            if (datetime != null && datetime.length() > 0) {
                String[] s = datetime.split("(\\s+|\\-|\\:)+");  // Tue, 14 Feb 2012 10:33:07 GMT
                if (s.length >= 7) {
                    Time time = new Time(s[7]);
                    time.set(Integer.valueOf(s[6]), Integer.valueOf(s[5]), Integer.valueOf(s[4]),
                             Integer.valueOf(s[1]), MONTH.get(s[2]), Integer.valueOf(s[3]));
                    return time.toMillis(true);
                }
            }
        } catch (Throwable ex) {
            Log.w(TAG, "parseDateTime", ex);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ListItem {"+
                " fullPath='"+fullPath+'\''+
                ", displayName='"+displayName+'\''+
                ", contentLength="+contentLength+
                ", lastUpdated="+lastUpdated+
                ", isCollection="+isCollection+
                ", etag="+etag+
                ", contentType="+contentType+
                ", shared="+shared+
                ", ownerName="+ownerName+
                ", aliasEnabled="+aliasEnabled+
                ", readOnly="+readOnly+
                ", visible="+visible+
                ", publicUrl="+publicUrl+
                ", etime="+etime+
                ", mediaType="+mediaType+
                ", mpfsFileId="+mpfsFileId+
                ", hasThumbnail="+hasThumbnail+
                " }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(fullPath);
        parcel.writeString(displayName);
        parcel.writeLong(contentLength);
        parcel.writeLong(lastUpdated);
        parcel.writeByte((byte) (isCollection ? 1 : 0));
        parcel.writeString(etag);
        parcel.writeByte((byte) (aliasEnabled ? 1 : 0));
        parcel.writeString(contentType);
        parcel.writeByte((byte) (shared ? 1 : 0));
        parcel.writeByte((byte) (readOnly ? 1 : 0));
        parcel.writeString(ownerName);
        parcel.writeString(publicUrl);
        parcel.writeLong(etime);
        parcel.writeString(mediaType);
        parcel.writeString(mpfsFileId);
        parcel.writeByte((byte) (hasThumbnail ? 1 : 0));
    }

    public static final Parcelable.Creator<ListItem> CREATOR = new Parcelable.Creator<ListItem>() {

        public ListItem createFromParcel(Parcel parcel) {
            return new ListItem(parcel.readString(), parcel.readString(), parcel.readLong(),
                                parcel.readLong(), parcel.readByte() > 0, parcel.readString(), parcel.readByte() > 0,
                                parcel.readString(), parcel.readByte() > 0, parcel.readByte() > 0,
                                parcel.readString(), parcel.readString(), parcel.readLong(), parcel.readString(),
                                parcel.readString(), parcel.readByte() > 0);
        }

        public ListItem[] newArray(int size) {
            return new ListItem[size];
        }
    };

    public String getFullPath() {
        return fullPath;
    }

    public String getName() {
        return new File(fullPath).getName();
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getContentLength() {
        return contentLength;
    }

    /**
     * @return time in milliseconds
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEtag() {
        return etag;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isAliasEnabled() {
        return aliasEnabled;
    }

    public boolean isShared() {
        return shared;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public long getEtime() {
        return etime;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getMpfsFileId() {
        return mpfsFileId;
    }

    public boolean hasThumbnail() {
        return hasThumbnail;
    }
}
