(ns tweet-match-calc.core
  (:require [tweet-match-calc.db.mysql :as mysql]
            [tweet-match-calc.db.mongo :as mongo]
            [tweet-match-calc.calc.morpho :as morpho]
            [tweet-match-calc.calc.leven :as leven]
            [tweet-match-calc.api.twitter :as twitter]
            [clojure.string :as str]))


;; API利用用twitterアカウント
(def tw-accounts (read-string (slurp "config/twitter.clj")))

;; テストデータ
;; (def b-1 ["kanojo_hoshi_" "eclipse_rkt" "BaskeHi" "ilikesicp" "nobkz" "takuya199850" "mug_en" "charaoyukirin" "tetsuya1975m" "doumonnd"])
;; (def g-1 ["kareshi_hoshi_" "namida1055" "0428hrChi" "nemukyun1" "chomado" "Zwei_Megu" "xion_2574" "ManyaRiko" "gyuunyuu_umai" "tamakazura_yuri"])

;; １ユーザー毎の、相性ランキング、頻出ワードを取得する
(defn get-analyses [user candidates twitters sex]
  (loop [i 0
         analyses (atom [])
         user-data (morpho/get-tweet-analyze (Long/parseLong (:user_id user))
                                             (nth twitters (- (count twitters) 1)))]
    (when (< i (count candidates))
      (let [twitter (nth twitters (rem i (count twitters)))
            candidate (nth candidates i)]
        (Thread/sleep 30)
        (let [candidate-data (morpho/get-tweet-analyze (Long/parseLong (:user-id candidate))
                                                       twitter)]
          (swap! analyses conj (array-map :screen-name (:screen-name candidate)
                                          :profile-image (:profile-image candidate-data)
                                          :user-name (:user-name candidate-data)
                                          :leven (leven/levenshtein-distance (:text user-data)
                                                                             (:text candidate-data))))
          (if (= (+ i 1) (count candidates))
            (do
              (mongo/add-data (:user_id user)
                              (:screen_name user)
                              (:top-words user-data)
                              (sort-by :leven @analyses)
                              sex
                              (:profile-image user-data)
                              (:user-name user-data))
              (println
               (array-map :screen-name (:screen_name user)
                          :user-name (:user-name user-data)
                          :prof-img (:profile-image user-data)
                          :top-words (:top-words user-data)
                          :ranking (sort-by :leven @analyses))))
            (recur (inc i) analyses user-data)))))))

;; バッチ処理用
(defn go-matching [users twitters sex]
  (if (= sex "b")
    (map #(get-analyses % (mongo/get-rnd-user "g") twitters sex) users)
    (map #(get-analyses % (mongo/get-rnd-user "b") twitters sex) users)))


;; 解析のテスト
;(go-matching (mysql/select-boys) tw-accounts "b")
;(go-matching (mysql/select-girls) tw-accounts "g")
