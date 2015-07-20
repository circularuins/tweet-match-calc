(ns tweet-match-calc.api.twitter
  (:require [clojure.string :as str])
  (:import [twitter4j conf.ConfigurationBuilder TwitterFactory Twitter Query TwitterException Paging]))

(defn make-config [auth]
  (let [config (new ConfigurationBuilder)]
    (. config setOAuthConsumerKey (auth :consumer-key))
    (. config setOAuthConsumerSecret (auth :consumer-secret))
    (. config setOAuthAccessToken (auth :access-token))
    (. config setOAuthAccessTokenSecret (auth :access-token-secret))
    (. config build)))

(defn make-twitter [auth]
  (let [config (make-config auth)
        factory (new TwitterFactory config)]
    (. factory getInstance)))

(def paging
   (Paging. (int 1) (int 100)))

;; ユーザータイムラインの取得
(defn get-tweets [user-id auth]
  (try
    (let [twitter (make-twitter auth)
          tweets (.getUserTimeline twitter user-id paging)]
      (array-map :profile-image
                 (->>
                  tweets
                  (map #(.getUser %))
                  (first)
                  (.getOriginalProfileImageURL))
                 :profile-back-url
                 (->>
                  tweets
                  (map #(.getUser %))
                  (first)
                  (.getProfileBannerRetinaURL))
                 :user-name
                 (->>
                  tweets
                  (map #(.getUser %))
                  (first)
                  (.getName))
                 :text
                 (->>
                  tweets
                  (map #(.getText %))
                  str/join)))
    (catch Exception e (str "caught exception: " (.getMessage e)))))
