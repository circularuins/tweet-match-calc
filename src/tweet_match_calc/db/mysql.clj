(ns tweet-match-calc.db.mysql
  (:use [korma.db]
        [korma.core])
  (:require [clj-time.core :as t]
            [clj-time.local :as tl]
            [clj-time.format :as tf]
            [clojure.string :as str]))

(def custom-formatter (tf/formatter "yyyy/MM/dd HH:mm:ss"))
(def minutes-ago (t/minus (tl/local-now) (t/minutes 1)))

(defdb twitter-db
  (mysql
   {:db "twitter_bot"
    :port "3306"
    :user "wake"
    :password "wakenatsuhiko"}))

(defentity kanojo_hoshi)
(defentity kareshi_hoshi)

;; (defn select-by-name [name]
;;   (select kanojo_hoshi
;;           (fields :name :screen_name :user_id :date)
;;           (where {:name name})
;;           (order :date :DESC)))

(defn select-boys []
  (->>
   (select kanojo_hoshi
           (fields :name :screen_name :user_id :date :batch :tweet :date :description)
           (order :no :DESC)
           (limit 50))
   (filter #(= 0 (:batch %)))))

(defn select-girls []
  (->>
   (select kareshi_hoshi
           (fields :name :screen_name :user_id :date :batch :tweet :date :description)
           (order :no :DESC)
           (limit 50))
   (filter #(= 0 (:batch %)))))

(defn get-rnd-boys
  [num]
  (->>
   (select kanojo_hoshi
           (fields :name :screen_name :user_id :date :batch :tweet :date :description)
           (where {:batch 0})
           (order :no :DESC)
           (limit 10000))
   (shuffle)
   (take num)))

(defn get-rnd-girls
  [num]
  (->>
   (select kareshi_hoshi
           (fields :name :screen_name :user_id :date :batch :tweet :date :description)
           (where {:batch 0})
           (order :no :DESC)
           (limit 10000))
   (shuffle)
   (take num)))

(defn complete-matching
  [user-id sex]
  (if (= sex "b")
    (update kanojo_hoshi
            (set-fields {:batch 1})
            (where {:user_id user-id}))
    (update kareshi_hoshi
            (set-fields {:batch 1})
            (where {:user_id user-id}))))
