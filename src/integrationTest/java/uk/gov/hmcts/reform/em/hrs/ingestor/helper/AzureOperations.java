package uk.gov.hmcts.reform.em.hrs.ingestor.helper;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.devskiller.jfairy.Fairy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class AzureOperations {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureOperations.class);

    private final BlobContainerClient blobContainerClient;
    private final Fairy fairy;

    @Autowired
    public AzureOperations(@Qualifier("cvpBlobContainerClient") BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
        fairy = Fairy.create();
    }

    public void uploadToContainer(final Set<String> blobNames) {
        blobNames.forEach(this::uploadToContainer);
    }

    public void uploadToContainer(final String blobName) {
        final String content = fairy.textProducer().sentence();
        uploadToContainer(blobName, content);
    }

    public void uploadToContainer(final String blobName, final String content) {
        uploadToContainer(blobName, content.getBytes(StandardCharsets.UTF_8));
    }

    public void uploadToContainer(final String blobName, final byte[] data) {
        final InputStream inStream = new ByteArrayInputStream(data);

        final BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.upload(new BufferedInputStream(inStream), data.length);
        LOGGER.info("Blob '{}' uploaded successfully", blobName);
    }

    //  Azure blobstore API doesn't support 'deleteBlob'.  We can only mark a blob for deletion
    public void clearContainer() {
        blobContainerClient.listBlobs()
            .forEach(x -> x.setDeleted(true));
    }
}
