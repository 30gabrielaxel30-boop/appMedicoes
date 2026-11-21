package com.keyhistory.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.keyhistory.BuildConfig
import com.keyhistory.R
import com.keyhistory.components.InfoRow
import com.keyhistory.components.KhCard
import com.keyhistory.components.KhColors
import com.keyhistory.components.KhTopBar

private val privacySections = listOf(
    "Quem controla os dados" to "Você. O KeyHistory é um aplicativo de uso pessoal: registra somente o que o próprio usuário do aparelho digita com o teclado KeyHistory, após consentimento explícito.",
    "O que é registrado" to "Apenas o texto digitado por meio do teclado KeyHistory em campos de texto comuns, junto com data, horário, aplicativo de origem (quando o Android informa) e uma categoria. O conteúdo que já existia no campo nunca é lido.",
    "O que NUNCA é registrado" to "Senhas, PINs, campos numéricos e telefônicos, campos marcados pelo aplicativo como não pessoais (modo anônimo), campos cuja dica indique dado sensível (código, cartão, CPF, token, chave Pix…), campos do próprio KeyHistory, campos de origem não verificável e conteúdos que aparentem ser números de cartão, CPF/CNPJ, códigos de verificação ou tokens.",
    "Onde os dados ficam" to "Exclusivamente no armazenamento privado deste aparelho, em banco de dados Room. O aplicativo não declara a permissão de Internet e, portanto, não consegue transmitir dados. Backup em nuvem e transferência entre aparelhos estão desativados.",
    "Criptografia" to "Cada registro é cifrado com AES-256-GCM. A chave de dados é protegida por uma chave mestra gerada e mantida no Android Keystore, que não pode ser exportada. A pesquisa usa um índice cego (HMAC-SHA256), sem guardar texto legível.",
    "Controle" to "Você pode pausar o registro no próprio teclado, desativá-lo nas configurações, excluir aplicativos do registro, apagar registros individuais, por período, por aplicativo ou o histórico inteiro. A desinstalação apaga todos os dados e chaves.",
    "Exportação e cópia" to "Somente por ação explícita sua. A exportação usa o seletor de arquivos do Android e grava um CSV não criptografado no local escolhido — proteja esse arquivo.",
    "Uso responsável" to "Não utilize o KeyHistory para monitorar outras pessoas. Instale-o apenas no seu próprio aparelho.",
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Política de privacidade", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            privacySections.forEach { (title, body) ->
                KhCard {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = KhColors.TextMuted)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(containerColor = KhColors.Background, topBar = { KhTopBar("Sobre", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(88.dp))
            Text("KeyHistory", style = MaterialTheme.typography.headlineMedium)
            Text("Histórico pessoal, local e criptografado do que você digita.", color = KhColors.TextMuted,
                style = MaterialTheme.typography.bodyMedium)
            KhCard(Modifier.fillMaxWidth()) {
                InfoRow("Versão", BuildConfig.VERSION_NAME)
                InfoRow("Build", BuildConfig.VERSION_CODE.toString())
                InfoRow("Pacote", BuildConfig.APPLICATION_ID)
                InfoRow("Android mínimo", "10 (API 29)")
                InfoRow("Armazenamento", "Local (Room)")
                InfoRow("Criptografia", "AES-256-GCM + Keystore")
                InfoRow("Rede", "Nenhuma (sem permissão)")
            }
            KhCard(Modifier.fillMaxWidth()) {
                Text("Como usar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "1. Ative o teclado KeyHistory nas configurações do Android.",
                    "2. Selecione-o como teclado atual.",
                    "3. No painel, toque em \"Ativar registro\" e aceite o termo.",
                    "4. Digite normalmente. O indicador verde no teclado mostra quando o registro está ativo; vermelho indica campo protegido.",
                    "5. O texto é salvo ao tocar em Enviar/OK, ao sair do campo, quando o app limpa o campo, ou pelo botão Salvar do teclado.",
                    "6. Consulte, pesquise, copie, exporte ou apague no aplicativo.",
                ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = KhColors.TextMuted, modifier = Modifier.padding(vertical = 3.dp)) }
            }
        }
    }
}
