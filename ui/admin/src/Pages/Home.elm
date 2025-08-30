module Pages.Home exposing (view)

import Html exposing (..)
import Html.Attributes as A
import Route

view : Html msg
view =
    div [ A.class "home" ]
        [ h1 [] [ text "Administration" ]
        , div [ A.class "actions" ]
            [ a [ A.href (Route.href Route.RHops), A.class "action" ] [ text "Gérer les houblons" ]
            , a [ A.href (Route.href Route.RUpload), A.class "action" ] [ text "Charger des données (CSV)" ]
            , a [ A.href (Route.href Route.RReferentials), A.class "action" ] [ text "Gérer les référentiels" ]
            ]
        ]
