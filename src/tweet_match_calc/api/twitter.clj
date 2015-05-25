(ns tweet-match-calc.api.twitter
  (:require [clojure.string :as str])
  (:import [twitter4j conf.ConfigurationBuilder TwitterFactory Twitter Query TwitterException Paging]))

;; consumer-key, consumer-secret, access-token, access-token-secret の読み込み
(def auth-data (read-string (slurp "config/twitter.clj")))

(defn make-config []
  (let [config (new ConfigurationBuilder)]
    (. config setOAuthConsumerKey (auth-data :consumer-key))
    (. config setOAuthConsumerSecret (auth-data :consumer-secret))
    (. config setOAuthAccessToken (auth-data :access-token))
    (. config setOAuthAccessTokenSecret (auth-data :access-token-secret))
    (. config build)))

(defn make-twitter []
  (let [config (make-config)
        factory (new TwitterFactory config)]
    (. factory getInstance)))

(def paging
   (Paging. (int 1) (int 100)))

(defn get-tweets [screen-name]
  (let [twitter (make-twitter)
        tweets (.getUserTimeline twitter screen-name paging)]
    (->>
     tweets
     (map #(.getText %))
     str/join)))
