module Pages.Login exposing (Model, Msg(..), init, update, view)

import Api
import Browser.Navigation as Nav
import Html exposing (Html, button, div, input, label, text)
import Html.Attributes as A
import Html.Events as E
import Http
import Route


-- MODEL

type alias Model =
    { email : String
    , password : String
    , error : Maybe String
    , busy : Bool
    , csrf : Maybe String
    }


init : Maybe String -> Model
init csrf =
    { email = ""
    , password = ""
    , error = Nothing
    , busy = False
    , csrf = csrf
    }


-- MSG

type Msg
    = SetEmail String
    | SetPassword String
    | Submit
    | GotLogin (Result Http.Error Api.Token)


-- UPDATE

update : Nav.Key -> Msg -> Model -> ( Model, Cmd Msg )
update key msg model =
    case msg of
        SetEmail s ->
            ( { model | email = s }, Cmd.none )

        SetPassword s ->
            ( { model | password = s }, Cmd.none )

        Submit ->
            ( { model | busy = True, error = Nothing }
            , Api.login { email = model.email, password = model.password } model.csrf GotLogin
            )

        GotLogin (Ok _) ->
            ( { model | busy = False }
            , Nav.pushUrl key (Route.href Route.RHops)
            )

        GotLogin (Err e) ->
            ( { model | busy = False, error = Just (httpErrorToString e) }, Cmd.none )


-- VIEW

view : Model -> Html Msg
view model =
    div [ A.class "login-wrapper card" ]
        [ label [ A.for "email" ] [ text "Email" ]
        , input
            [ A.id "email"
            , A.type_ "email"
            , A.value model.email
            , E.onInput SetEmail
            , A.attribute "autocomplete" "username"
            ]
            []
        , label [ A.for "password" ] [ text "Mot de passe" ]
        , input
            [ A.id "password"
            , A.type_ "password"
            , A.value model.password
            , E.onInput SetPassword
            , A.attribute "autocomplete" "current-password"
            ]
            []
        , button [ A.type_ "button", E.onClick Submit, A.disabled model.busy ]
            [ text (if model.busy then "Connexion…" else "Se connecter") ]
        , case model.error of
            Nothing ->
                text ""

            Just e ->
                div [ A.class "error" ] [ text e ]
        ]


-- HELPERS

httpErrorToString : Http.Error -> String
httpErrorToString err =
    case err of
        Http.BadUrl _ ->
            "URL invalide"

        Http.Timeout ->
            "Timeout"

        Http.NetworkError ->
            "Erreur réseau"

        Http.BadStatus code ->
            if code == 401 then
                "Identifiants invalides"
            else
                "Erreur (" ++ String.fromInt code ++ ")"

        Http.BadBody _ ->
            "Réponse illisible"
