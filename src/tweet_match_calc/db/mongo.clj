(ns tweet-match-calc.db.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :as mo]
            [clojure.string :as str]
            [clj-time
             [core :as t]
             [local :as tl]
             [coerce :as coerce]])
  (:import java.util.Date
           java.util.regex.Pattern
           org.bson.types.ObjectId))

(defn fix-object [object]
  (let [id (:_id object)]
    (-> object
        (assoc :id (str id))
        (dissoc :_id))))

(def db (mg/get-db (mg/connect) "matching-db"))

(defn add-data
  [id screen-name top-words ranking sex profile-image user-name tweet date description profile-back-url]
  (let [coll "mach-ranking"]
    (mc/update db coll {:user-id id}
               {:user-id id
                :screen-name screen-name
                :top-words top-words
                :ranking ranking
                :sex sex
                :profile-image profile-image
                :user-name user-name
                :calc-date (.toString (tl/local-now))
                :tweet tweet
                :date date
                :description description
                :profile-back-url profile-back-url
                :pv 0}
               {:upsert true})))

(defn add-couple-data
  [id partner-screen-name best-leven date]
  (let [coll "best-matching"]
    (mc/update db coll {:user-id id}
               {:user-id id
                :partner-screen-name partner-screen-name
                :best-leven best-leven
                :date date}
               {:upsert true})))

(defn get-data
  [screen-name]
  (->> (mq/with-collection db "mach-ranking"
         (mq/find {:screen-name screen-name})
         (mq/sort (array-map :date 1)))
       (map fix-object)))

(defn get-rnd-user
  [sex]
  (->> (mq/with-collection db "mach-ranking"
         (mq/find {:sex sex})
         (mq/fields [:screen-name :user-id]))
       (shuffle)
       (take 10)))

(defn get-best-couple
  []
  (nth (->> (mq/with-collection db "best-matching"
              (mq/find {:best-leven {mo/$gte 600}})
              (mq/sort (array-map :best-leven 1))
              (mq/limit 1)))
       0))



;; 補助関数

(defn all-clear [coll]
  (mc/remove db coll))

(defn count-all [coll]
  (mc/count db coll))

