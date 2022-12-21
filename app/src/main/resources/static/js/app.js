$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: $(this).serialize(),
                    success: function (msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
        $("#shortcsv").submit(
            function (event) {
                event.preventDefault();

                let csv = document.getElementById("formFile").files[0]
                const formData = new FormData();
                formData.append('file', csv);

                const fetch_csv = fetch('http://localhost:8080/api/bulk', {
                    method: 'POST',
                    body: formData
                })

                fetch_csv.then(response => response.blob())
                    .then(blob => URL.createObjectURL(blob))
                    .then(uril => {
                        var link = document.createElement("a");
                        link.href = uril;
                        link.download = "shortURLs.csv";
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                    })
                    .catch((error) => {
                        console.error('Error:', error);
                    });
            });
    });