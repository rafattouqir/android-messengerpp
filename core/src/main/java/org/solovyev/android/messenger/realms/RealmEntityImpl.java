package org.solovyev.android.messenger.realms;

import android.os.Parcel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.solovyev.common.JCloneable;
import org.solovyev.common.JObject;
import org.solovyev.common.text.Strings;

public class RealmEntityImpl extends JObject implements JCloneable<RealmEntityImpl>, RealmEntity {

    /*
    **********************************************************************
    *
    *                           CONSTANTS
    *
    **********************************************************************
    */

    public static final String DELIMITER = "_";
    public static final String DELIMITER_REALM = "~";

    public static final Creator <RealmEntity> CREATOR = new Creator<RealmEntity>() {
        @Override
        public RealmEntity createFromParcel(@Nonnull Parcel in) {
            return null;
        }

        @Override
        public RealmEntity[] newArray(int size) {
            return new RealmEntity[size];
        }
    };

    /*
    **********************************************************************
    *
    *                           FIELDS
    *
    **********************************************************************
    */

    @Nonnull
    private String realmId;

    @Nullable
    private String realmDefId;

    @Nonnull
    private String realmEntityId;

    @Nonnull
    private String entityId;

    /*
    **********************************************************************
    *
    *                           CONSTRUCTORS
    *
    **********************************************************************
    */

    private RealmEntityImpl(@Nonnull String realmId, @Nonnull String realmEntityId) {
        this.realmId = realmId;
        this.realmEntityId = realmEntityId;
        this.entityId = realmId + DELIMITER + realmEntityId;
    }

    private RealmEntityImpl(@Nonnull String realmId,
                            @Nullable String realmDefId,
                            @Nonnull String realmEntityId,
                            @Nonnull String entityId) {
        this.realmId = realmId;
        this.realmDefId = realmDefId;
        this.realmEntityId = realmEntityId;
        this.entityId = entityId;
    }

    @Nonnull
    public static RealmEntity newInstance(@Nonnull String realmId, @Nonnull String realmEntityId) {
        if (Strings.isEmpty(realmId)) {
            throw new IllegalArgumentException("Realm cannot be empty!");
        }

        if (Strings.isEmpty(realmEntityId)) {
            throw new IllegalArgumentException("Realm entity id cannot be empty!");
        }

        return new RealmEntityImpl(realmId, realmEntityId);
    }

    @Nonnull
    public static RealmEntity fromEntityId(@Nonnull String entityId) {
        final int index = entityId.indexOf(DELIMITER);
        if ( index >= 0 ) {
            final String realmId = entityId.substring(0, index);
            final String realmUserId = entityId.substring(index + 1);
            return newInstance(realmId, realmUserId);
        } else {
            throw new IllegalArgumentException("No realm is stored in entityId!");
        }
    }

    @Nonnull
    private static RealmEntity fromParcel(@Nonnull Parcel in) {
        return new RealmEntityImpl(in.readString(), in.readString(), in.readString(), in.readString());
    }

    @Nonnull
    public String getEntityId() {
        return entityId;
    }

    @Nonnull
    public String getRealmId() {
        return this.realmId;
    }

    @Nonnull
    @Override
    public String getRealmDefId() {
        if ( this.realmDefId == null ) {
            final int index = realmId.indexOf(DELIMITER_REALM);
            if ( index >= 0 ) {
                this.realmDefId = entityId.substring(0, index);
            } else {
                throw new IllegalArgumentException("No realm def id is stored in realmId!");
            }

        }
        return this.realmDefId;
    }

    @Nonnull
    public String getRealmEntityId() {
        return this.realmEntityId;
    }

    @Nonnull
    @Override
    public RealmEntityImpl clone() {
        return (RealmEntityImpl) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RealmEntityImpl)) return false;

        RealmEntityImpl that = (RealmEntityImpl) o;

        if (!entityId.equals(that.entityId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return entityId.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@Nonnull Parcel out, int flags) {
        out.writeString(realmId);
        out.writeString(realmDefId);
        out.writeString(realmEntityId);
        out.writeString(entityId);
    }

    /*
    **********************************************************************
    *
    *                           STATIC
    *
    **********************************************************************
    */

    @Nonnull
    public static String getRealmId(@Nonnull String realmDefId, @Nonnull int index) {
        return realmDefId + DELIMITER_REALM + index;
    }
}