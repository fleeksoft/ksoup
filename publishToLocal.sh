./gradlew clean
./gradlew :ksoup-engine-common:publishToMavenLocal
./gradlew :ksoup-engine-kotlinx:publishToMavenLocal
./gradlew :ksoup-engine-korlibs:publishToMavenLocal

./gradlew clean
./gradlew :ksoup:publishToMavenLocal -PisKorlibs=false
./gradlew :ksoup-network:publishToMavenLocal -PisKorlibs=false

./gradlew clean
./gradlew :ksoup:publishToMavenLocal -PisKorlibs=true
./gradlew :ksoup-network-korlibs:publishToMavenLocal -PisKorlibs=true