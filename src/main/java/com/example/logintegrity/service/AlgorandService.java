package com.example.logintegrity.service;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AlgorandService {
    private final String url, token, mnemonic, indexerUrl; private final boolean writes, demoMode;
    public AlgorandService(@Value("${algorand.algod-url:https://testnet-api.algonode.cloud}") String url,
                           @Value("${algorand.algod-token:}") String token,
                           @Value("${algorand.mnemonic:}") String mnemonic,
                           @Value("${algorand.indexer-url:https://testnet-idx.algonode.cloud}") String indexerUrl,
                           @Value("${algorand.writes-enabled:false}") boolean writes,
                           @Value("${algorand.demo-mode:true}") boolean demoMode) {
        this.url=url; this.token=token; this.mnemonic=mnemonic; this.indexerUrl=indexerUrl; this.writes=writes; this.demoMode=demoMode;
    }
    private AlgodClient algod() { return new AlgodClient(url, 443, token == null ? "" : token); }
    public Map<String,Object> status() {
        if (demoMode) return Map.of("connected", false, "network", "Algorand TestNet (simulated)", "demoMode", true, "lastRound", 0L, "writesEnabled", false, "accountConfigured", false, "accountLabel", "SecureLog Demo Account", "message", "No real wallet or on-chain transactions are used in demo mode.");
        try {
            var node = algod().GetStatus().execute();
            return Map.of("connected", node.isSuccessful(), "network", "TestNet", "lastRound", node.body().lastRound == null ? 0L : node.body().lastRound,
                    "writesEnabled", writes, "accountConfigured", mnemonic != null && !mnemonic.isBlank());
        } catch (Exception e) { return Map.of("connected", false, "network", "TestNet", "writesEnabled", writes, "accountConfigured", mnemonic != null && !mnemonic.isBlank(), "error", e.getMessage() == null ? "Algod unavailable" : e.getMessage()); }
    }
    public String accountAddress() {
        if (demoMode) return "SecureLog Demo Account (placeholder only)";
        if (mnemonic == null || mnemonic.isBlank()) return "Not configured";
        try { return new Account(mnemonic).getAddress().toString(); } catch (Exception e) { return "Invalid mnemonic"; }
    }
    public ChainReceipt anchor(String hash) {
        if (demoMode) return new ChainReceipt(null, null, "DEMO_ONLY");
        if (!writes) return new ChainReceipt(null, null, "PENDING_CONFIGURATION");
        if (mnemonic == null || mnemonic.isBlank()) throw new IllegalStateException("Set ALGORAND_MNEMONIC for a dedicated TestNet account.");
        try {
            AlgodClient algod = algod(); Account account = new Account(mnemonic); Address sender = account.getAddress();
            Transaction tx = Transaction.PaymentTransactionBuilder().lookupParams(algod).sender(sender).receiver(sender)
                    .amount(0).noteUTF8("SECLOG-SHA256:" + hash).build();
            SignedTransaction signed = account.signTransaction(tx);
            Response<PostTransactionsResponse> response = algod.RawTransaction().rawtxn(Encoder.encodeToMsgPack(signed)).execute();
            if (!response.isSuccessful()) throw new IllegalStateException("Algorand rejected transaction (HTTP response was not successful).");
            return new ChainReceipt(response.body().txId, null, "SUBMITTED");
        } catch (Exception e) { throw new IllegalStateException("Algorand anchoring failed: " + e.getMessage(), e); }
    }
    public boolean verify(String txId, String expectedHash) {
        if (demoMode) return false;
        if (txId == null || txId.isBlank()) return false;
        try {
            Map result = RestClient.builder().baseUrl(indexerUrl).build().get().uri(uri -> uri.path("/v2/transactions").queryParam("txid", txId).build()).retrieve().body(Map.class);
            List<Map> txs = (List<Map>) result.get("transactions");
            if (txs == null || txs.isEmpty()) return false;
            Object note = txs.get(0).get("note"); if (!(note instanceof String)) return false;
            String decoded = new String(Base64.getDecoder().decode((String) note), StandardCharsets.UTF_8);
            return decoded.equals("SECLOG-SHA256:" + expectedHash);
        } catch (Exception e) { return false; }
    }
    public boolean demoMode() { return demoMode; }
    public record ChainReceipt(String transactionId, Long confirmedRound, String status) {}
}
