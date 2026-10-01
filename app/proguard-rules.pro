# Project-specific R8 rules. Room, DataStore, Firebase, Play Billing, Mobile Ads and UMP all
# ship consumer rules in their AARs, so only app-level concerns live here.

# Readable Crashlytics stack traces (the mapping file is uploaded by the Crashlytics plugin).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kept for Crashlytics custom exception reporting and annotation-driven libraries.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
